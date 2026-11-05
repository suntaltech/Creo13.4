"use strict";
// Offline validation of user-provided response bodies. No HTTP, DOM, Creo, or mock server.
const fs = require("node:fs");
const crypto = require("node:crypto");
const assert = require("node:assert/strict");
const path = require("node:path");
const { assess } = require("./reference/sheetmetal-assessment.js");
assert.ok(process.argv[2], "Usage: node verify-raw-responses.cjs <response-json>");
const bytes = fs.readFileSync(process.argv[2]);
const data = JSON.parse(bytes.toString("utf8").replace(/^\uFEFF/, ""));
for (const name of ["fileinfo", "units", "surfaces", "bbox"]) {
  assert.equal(data[name]?.status?.error, false, name + " did not report success");
  assert.ok(data[name].data && typeof data[name].data === "object", name + " data missing");
}
const faces = data.surfaces.data.surflist;
assert.ok(Array.isArray(faces) && faces.length > 0, "No surface records");
const profiles = {};
for (const face of faces) {
  const profile = Object.keys(face).sort().join(","); profiles[profile] = (profiles[profile] || 0) + 1;
  assert.ok(Number.isFinite(face.area) && face.area > 0, "Invalid area");
  assert.ok(Number.isFinite(face.surface_id), "Invalid surface id");
  for (const axis of ["x", "y", "z"]) {
    assert.ok(Number.isFinite(face.min_extent?.[axis]) && Number.isFinite(face.max_extent?.[axis]), "Invalid extent");
    assert.ok(face.max_extent[axis] >= face.min_extent[axis], "Reversed extent");
  }
}
assert.equal(new Set(faces.map(face => face.surface_id)).size, faces.length, "Duplicate surface ids");
const result = assess({ model: data.fileinfo.data.file, revision: data.fileinfo.data.revision, units: data.units.data.units, surfaces: faces, box: data.bbox.data });
assert.equal(result.nativeSheetmetalState, "UNKNOWN");
assert.equal(result.thinWallShapeAssessment, "INSUFFICIENT_EVIDENCE");
assert.equal(result.uniformThicknessAssessment, "INSUFFICIENT_EVIDENCE");
assert.equal(result.estimatedThickness, null); assert.equal(result.suggestedMode, null);
const legacyCount = faces.filter(face => ["x", "y", "z"].some(axis => Math.abs(face.max_extent[axis] - face.min_extent[axis]) <= 0.01)).length;
console.log(JSON.stringify({
  type: "OFFLINE_REAL_RESPONSE_REPLAY_NOT_RUNTIME_EXECUTION",
  inputSha256: crypto.createHash("sha256").update(bytes).digest("hex"),
  responseGroupsChecked: 4,
  surfaceRecordCount: faces.length,
  surfaceFieldProfiles: profiles,
  surfaceIdsUnique: true,
  areasAndExtentsFiniteAndValid: true,
  assessmentModuleSha256: crypto.createHash("sha256").update(fs.readFileSync(path.join(__dirname, "reference/sheetmetal-assessment.js"))).digest("hex"),
  assessedResult: result,
  historicalHeuristicReproduction: { count: legacyCount, rule: "any axis extent span <= 0.01 in response model units", actualPlanarityEstablished: false, usedForClassification: false },
  dirnameAndRawSurfacesRepublished: false,
  noNewNetworkOrModelCalls: true
}, null, 2));
