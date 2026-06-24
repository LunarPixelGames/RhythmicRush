export function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === "object" && value !== null && !Array.isArray(value);
}

export function requireString(
  value: unknown,
  fieldName: string,
  minLength: number,
  maxLength: number,
): string {
  if (typeof value !== "string") throw new ValidationError(`${fieldName} must be a string.`);
  const result = value.trim();
  if (result.length < minLength || result.length > maxLength) {
    throw new ValidationError(
      `${fieldName} must contain between ${minLength} and ${maxLength} characters.`,
    );
  }
  return result;
}

export class ValidationError extends Error {}
