"use strict";
// Synthetic unit inputs only; not samples captured from Creo or HTTP.
const test = require("node:test");
const assert = require("node:assert/strict");
const { assess, analysisEvents } = require("../web/assets/js/sheetmetal-assessment.js");
function input(patch = {}) {
  return { model: "synthetic.prt", revision: 3, units: "mm", box: { xmin: 0, xmax: 80, ymin: 0, ymax: 30, zmin: 0, zmax: 2 }, surfaces: [{ surface_id: 1, area: 45 }], ...patch };
}
test("a small envelope extent does not establish thickness or a driving recommendation", () => {
  const r = assess(input());
  assert.equal(r.estimatedThickness, null); assert.equal(r.suggestedMode, null);
  assert.equal(r.uniformThicknessAssessment, "INSUFFICIENT_EVIDENCE");
});
test("a large minimum envelope extent does not default to shell", () => {
  const r = assess(input({ box: { xmin: 0, xmax: 337.5, ymin: 0, ymax: 75.75, zmin: 0, zmax: 389 } }));
  assert.equal(r.suggestedMode, null); assert.equal(r.sheetmetal, null);
});
test("permuting envelope axes cannot create a conversion recommendation", () => {
  for (const dims of [[1, 30, 80], [80, 1, 30], [30, 80, 1]]) {
    const r = assess(input({ box: { xmin: 0, xmax: dims[0], ymin: 0, ymax: dims[1], zmin: 0, zmax: dims[2] } }));
    assert.equal(r.thinWallShapeAssessment, "INSUFFICIENT_EVIDENCE"); assert.equal(r.suggestedMode, null);
  }
});
test("feature names cannot establish positive or negative native sheetmetal state", () => {
  for (const features of [[], [{ type: "WALL" }], [{ type: "IMPORT" }]]) {
    const r = assess(input({ features })); assert.equal(r.nativeSheetmetalState, "UNKNOWN"); assert.equal(r.sheetmetal, null);
  }
});
test("surface counts and largest area are retained without calling it a plane", () => {
  const r = assess(input({ surfaces: [{ surface_id: 7, area: 12 }, { surface_id: 9, area: 45 }] }));
  assert.equal(r.surfaceCount, 2); assert.deepEqual(r.largestSurface, { id: 9, area: 45 });
  assert.equal(r.planeCount, null); assert.equal(r.largestPlane, null);
});
test("missing units stay unconfirmed rather than silently becoming millimetres", () => {
  const r = assess(input({ units: null })); assert.equal(r.units, null); assert.ok(r.reasonCodes.includes("LENGTH_UNITS_UNCONFIRMED"));
});
test("known inch units remain inch and are not silently converted", () => {
  const r = assess(input({ units: " in " })); assert.equal(r.units, "in"); assert.equal(r.spans.z, 2);
});
test("missing surfaces fail instead of becoming a successful empty model", () => {
  assert.throws(() => assess(input({ surfaces: undefined })), { code: "INVALID_RESPONSE" });
});
test("invalid or nonnumeric bounds fail rather than being coerced", () => {
  for (const box of [{ xmin: 10, xmax: 1 }, { xmin: 0, xmax: "80" }, { xmin: NaN, xmax: 80 }]) assert.throws(() => assess(input({ box })), { code: "INVALID_RESPONSE" });
});
test("invalid surface entries fail; unavailable areas are not invented", () => {
  assert.throws(() => assess(input({ surfaces: [null] })), { code: "INVALID_RESPONSE" });
  const r = assess(input({ surfaces: [{ surface_id: 1, area: NaN }, { surface_id: 2, area: -1 }] }));
  assert.equal(r.largestSurface, null); assert.equal(r.surfacesWithoutUsableArea, 2);
});
test("non-part identities fail; no claimed body or plane is inferred", () => {
  assert.throws(() => assess(input({ model: "synthetic.asm" })), { code: "INVALID_RESPONSE" });
});
test("export excludes shared connection events and unknown credential fields", () => {
  const events = analysisEvents([{ event: "connected", sessionId: "synthetic-secret" }, { event: "smt_analysis_started", requestId: "synthetic-request", sessionId: "synthetic-secret", authorization: "synthetic-secret", error: "synthetic-secret" }], "synthetic-secret");
  assert.deepEqual(events, [{ event: "smt_analysis_started", requestId: "synthetic-request" }]);
});
test("export redacts a current session value even inside an otherwise allowed field", () => {
  const r = analysisEvents([{ event: "smt_analyze_http", model: "synthetic-secret.prt", reasonCodes: ["synthetic-secret"] }], "synthetic-secret");
  assert.equal(JSON.stringify(r).includes("synthetic-secret"), false);
});
