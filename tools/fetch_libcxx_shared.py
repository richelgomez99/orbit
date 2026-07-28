"""Extract libc++_shared.so for all 4 Android ABIs from the remote NDK zip,
reusing one central-directory read. Writes into app/src/main/jniLibs/<abi>/."""
import io, os, sys, urllib.request, zipfile

URL = "https://dl.google.com/android/repository/android-ndk-r27c-linux.zip"
JNI = sys.argv[1]  # app/src/main/jniLibs

# NDK triple -> Android ABI dir
ABIS = {
    "aarch64-linux-android": "arm64-v8a",
    "arm-linux-androideabi": "armeabi-v7a",
    "i686-linux-android": "x86",
    "x86_64-linux-android": "x86_64",
}

class HttpFile(io.RawIOBase):
    def __init__(self, url):
        self.url = url; self.pos = 0; self.read_bytes = 0
        with urllib.request.urlopen(urllib.request.Request(url, method="HEAD")) as r:
            self.size = int(r.headers["Content-Length"])
    def seekable(self): return True
    def seek(self, o, w=io.SEEK_SET):
        self.pos = o if w==io.SEEK_SET else (self.pos+o if w==io.SEEK_CUR else self.size+o); return self.pos
    def tell(self): return self.pos
    def read(self, n=-1):
        if n is None or n < 0: n = self.size - self.pos
        if n == 0: return b""
        end = min(self.pos+n, self.size)-1
        req = urllib.request.Request(self.url, headers={"Range": f"bytes={self.pos}-{end}"})
        with urllib.request.urlopen(req) as r: data = r.read()
        self.pos += len(data); self.read_bytes += len(data); return data
    def readinto(self, b):
        d = self.read(len(b)); b[:len(d)] = d; return len(d)

hf = HttpFile(URL)
zf = zipfile.ZipFile(hf)
names = zf.namelist()
for triple, abi in ABIS.items():
    member = next((n for n in names if n.endswith(f"{triple}/libc++_shared.so") and "/lib/" in n), None)
    if not member:
        print(f"WARN: {abi} ({triple}) not found"); continue
    data = zf.read(member)
    d = os.path.join(JNI, abi); os.makedirs(d, exist_ok=True)
    with open(os.path.join(d, "libc++_shared.so"), "wb") as f: f.write(data)
    print(f"{abi}: {len(data)} bytes")
print(f"total HTTP transferred: {hf.read_bytes/1024/1024:.1f} MB")
