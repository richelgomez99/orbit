export const ErrorCodes = {
  UNAUTHORIZED: "UNAUTHORIZED",
  VALIDATION_FAILED: "VALIDATION_FAILED",
  ATLAS_UNAVAILABLE: "ATLAS_UNAVAILABLE",
  NOT_FOUND: "NOT_FOUND",
  INSUFFICIENT_EVIDENCE: "INSUFFICIENT_EVIDENCE",
  INTERNAL: "INTERNAL",
} as const;

export type ErrorCode = (typeof ErrorCodes)[keyof typeof ErrorCodes];

export const UNAUTHORIZED_MESSAGES = {
  MISSING_OR_INVALID_HEADER: "Missing or invalid Authorization header",
  TOKEN_VERIFICATION_FAILED: "Token verification failed",
  INVALID_SUBJECT: "Invalid subject claim",
  INVALID_ROLE: "Invalid role claim",
} as const;

export type UnauthorizedMessage =
  (typeof UNAUTHORIZED_MESSAGES)[keyof typeof UNAUTHORIZED_MESSAGES];

export class UnauthorizedError extends Error {
  constructor(public readonly publicMessage: UnauthorizedMessage) {
    super(publicMessage);
    this.name = "UnauthorizedError";
  }
}
