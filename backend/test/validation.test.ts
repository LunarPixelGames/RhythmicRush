import { describe, expect, it } from "vitest";
import { isRecord, requireString, ValidationError } from "../src/shared/validation";

describe("validation", () => {
  it("accepts plain request objects", () => {
    expect(isRecord({ username: "Player_1" })).toBe(true);
    expect(isRecord(null)).toBe(false);
    expect(isRecord([])).toBe(false);
  });

  it("trims bounded strings", () => {
    expect(requireString(" Player_1 ", "username", 3, 20)).toBe("Player_1");
  });

  it("rejects strings outside the declared bounds", () => {
    expect(() => requireString("ab", "username", 3, 20)).toThrow(ValidationError);
    expect(() => requireString("x".repeat(21), "username", 3, 20)).toThrow(ValidationError);
  });
});
