"use strict";
/* Pure evidence assessment. No Creo, HTTP, DOM, file, or model-write calls. */
(function (root) {
  function invalid(message) {
    const error = new Error(message);
    error.code = "INVALID_RESPONSE";
    throw error;
  }

  function assess(input) {
    if (!input || typeof input.model !== "string" || !/\.prt$/i.test(input.model)) invalid("Expected a part identity");
    if (!Array.isArray(input.surfaces)) invalid("Surface list is unavailable");
    const spans = {};
    for (const axis of ["x", "y", "z"]) {
      const lo = input.box && input.box[axis + "min"];
      const hi = input.box && input.box[axis + "max"];
      if (!Number.isFinite(lo) || !Number.isFinite(hi) || hi < lo || !Number.isFinite(hi - lo)) invalid("Invalid bounding range");
      spans[axis] = hi - lo;
    }
    let largestSurface = null;
    let surfacesWithoutUsableArea = 0;
    for (const face of input.surfaces) {
      if (!face || typeof face !== "object" || Array.isArray(face)) invalid("Invalid surface record");
      if (!Number.isFinite(face.area) || face.area <= 0) {
        surfacesWithoutUsableArea++;
        continue;
      }
      if (!largestSurface || face.area > largestSurface.area) {
        const id = face.surface_id;
        largestSurface = { id: typeof id === "string" || Number.isFinite(id) ? id : null, area: face.area };
      }
    }
    const units = typeof input.units === "string" && input.units.trim() ? input.units.trim() : null;
    return {
      model: input.model,
      revision: input.revision == null ? null : input.revision,
      units,
      surfaceCount: input.surfaces.length,
      largestSurface,
      surfacesWithoutUsableArea,
      spans,
      nativeSheetmetalState: "UNKNOWN",
      thinWallShapeAssessment: "INSUFFICIENT_EVIDENCE",
      uniformThicknessAssessment: "INSUFFICIENT_EVIDENCE",
      estimatedThickness: null,
      suggestedMode: null,
      // Compatibility fields must not imply that absence of feature names proves a solid block.
      sheetmetal: null,
      planeCount: null,
      largestPlane: null,
      nativeConvertReady: false,
      saved: false,
      reasonCodes: [
        "NATIVE_SHEETMETAL_STATE_NOT_READ",
        "OPPOSED_SURFACE_AND_THICKNESS_EVIDENCE_UNAVAILABLE",
        "BOUNDING_BOX_IS_NOT_WALL_THICKNESS"
      ].concat(units ? [] : ["LENGTH_UNITS_UNCONFIRMED"])
    };
  }

  function analysisEvents(events, sessionSecret) {
    const fields = ["timestamp", "event", "requestId", "command", "function", "saved", "errorCode", "model", "revision", "units", "surfaceCount", "planeCount", "nativeConvertReady", "nativeSheetmetalState", "thinWallShapeAssessment", "uniformThicknessAssessment", "estimatedThickness", "suggestedMode", "reasonCodes", "revisionCheck", "blocked", "unitReadOutcome"];
    return events.filter(item => item && typeof item.event === "string" && item.event.startsWith("smt_")).map(item => {
      const selected = {};
      for (const key of fields) if (Object.prototype.hasOwnProperty.call(item, key)) selected[key] = item[key];
      return JSON.parse(JSON.stringify(selected, (key, value) => {
        if (/session.?id|token|authorization/i.test(key)) return "[REDACTED]";
        return typeof value === "string" && sessionSecret ? value.split(sessionSecret).join("[REDACTED]") : value;
      }));
    });
  }

  const api = Object.freeze({ assess, analysisEvents });
  if (typeof module !== "undefined" && module.exports) module.exports = api;
  else root.SiswSheetmetalAssessment = api;
})(typeof globalThis !== "undefined" ? globalThis : this);
