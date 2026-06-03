// IAuditLog.aidl
package com.orbit.app.data.ipc;

import com.orbit.app.data.ipc.AuditEntryParcel;

interface IAuditLog {
    List<AuditEntryParcel> entriesForDay(String isoDate);
    List<AuditEntryParcel> entriesForEnvelope(String envelopeId);
    int countForDay(String isoDate, String actionName);
    void appendEntry(in AuditEntryParcel entry);
}
