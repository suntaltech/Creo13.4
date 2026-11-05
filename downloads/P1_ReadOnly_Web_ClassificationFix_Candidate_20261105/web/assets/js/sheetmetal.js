"use strict";
/* Read-only evidence display. No inferred wall thickness or conversion-mode recommendation. */
(function () {
  const core = window.SiswCore;
  const assessment = window.SiswSheetmetalAssessment;
  const el = core.el;
  let busy = false;
  let blocked = false;
  let sequence = 0;

  function syncButtons() {
    el("smtAnalyzeBtn").disabled = busy || blocked;
    el("smtConvertBtn").disabled = true;
    el("smtLogBtn").disabled = busy || !core.events.some(item => item.event && item.event.startsWith("smt_"));
    document.querySelectorAll("input[name='smtMode']").forEach(radio => { radio.disabled = true; });
    el("smtThickness").disabled = true;
    el("smtSave").disabled = true;
    el("smtSave").checked = false;
  }

  function status(message, kind = "info") {
    el("smtStatus").textContent = message;
    el("smtStatus").className = "status " + kind;
  }

  function audit(event, details) {
    core.audit(event, Object.assign({ saved: false }, details));
  }

  function scalar(value, units, power) {
    if (!Number.isFinite(value) || value < 0) return "未取得";
    return value.toFixed(3) + (units ? " " + units + (power === 2 ? "²" : "") : "（模型单位未确认）");
  }

  function revision(value) {
    return typeof value === "string" || (typeof value === "number" && Number.isFinite(value)) ? value : null;
  }

  function render(data, revisionCheck) {
    const spans = data.spans;
    const face = data.largestSurface;
    el("smtResult").textContent = [
      "模型：" + data.model,
      "模型 revision：" + (data.revision == null ? "未提供；无法核对修订" : data.revision),
      "长度单位：" + (data.units || "未确认；不假定为 mm"),
      "原生钣金状态：未核验（不能用特征名称或导入历史判定）",
      "薄壁几何形态：证据不足；等厚状态：证据不足",
      "曲面数量：" + data.surfaceCount + "；真实平面数量：未核验",
      "有效面积记录中最大曲面：" + (face ? "ID " + (face.id == null ? "未提供" : face.id) + "，面积 " + scalar(face.area, data.units, 2) + "（未确认面类型）" : "无有效面积数据"),
      "包围尺寸：X " + scalar(spans.x, data.units) + "；Y " + scalar(spans.y, data.units) + "；Z " + scalar(spans.z, data.units),
      "壁厚：未测量；整体包围尺寸及用户参数都不能替代壁厚测量",
      "转换方式：不推荐抽壳或驱动方式，需进一步几何证据与人工确认",
      "导入的折弯薄壁件可能没有原生钣金历史；这不等于块状实体。",
      "模型前后核对：" + (revisionCheck === "FILE_AND_REVISION_MATCH" ? "文件与 revision 一致（不单独证明模型未修改）" : "文件一致，但 revision 未提供，核对范围有限"),
      "本页面只发出指定读取请求；转换及保存入口禁用，未执行原生转换。"
    ].join("\n");
    status("读取完成；钣金形态与等厚尚未确认，不推荐转换方式。", "info");
  }

  async function analyze() {
    if (busy || blocked) return;
    busy = true;
    core.setBusy(true);
    syncButtons();
    el("smtResult").textContent = "";
    status("正在通过 CREOSON 读取当前零件信息…");
    const requestId = "smt-analysis-" + Date.now() + "-" + (++sequence);
    audit("smt_analysis_started", { requestId });
    const read = async (command, fn, data = {}) => {
      const allowed = ["file/get_fileinfo", "geometry/get_surfaces", "geometry/bound_box", "file/get_length_units"];
      if (!allowed.includes(command + "/" + fn)) {
        const error = new Error("Non-read operation rejected");
        error.code = "READ_ONLY_VIOLATION";
        throw error;
      }
      audit("smt_read_started", { requestId, command, function: fn });
      try {
        const result = await core.call(command, fn, data, 60000);
        audit("smt_read_passed", { requestId, command, function: fn });
        return result;
      } catch (error) {
        audit("smt_read_failed", { requestId, command, function: fn });
        throw error;
      }
    };
    try {
      await core.ensureSession();
      const info = await read("file", "get_fileinfo");
      if (!info || typeof info.file !== "string" || !/\.prt$/i.test(info.file)) {
        const error = new Error("Not a part"); error.code = "NOT_PART"; throw error;
      }
      const surfaces = await read("geometry", "get_surfaces", { file: info.file });
      const box = await read("geometry", "bound_box", { file: info.file });
      let units = null;
      let unitReadOutcome = "UNCONFIRMED";
      try {
        const result = await read("file", "get_length_units", { file: info.file });
        if (result && typeof result.units === "string" && result.units.trim()) {
          units = result.units.trim(); unitReadOutcome = "RETURNED_UNIT_STRING";
        } else audit("smt_units_unconfirmed", { requestId, errorCode: "UNITS_RESPONSE_UNCONFIRMED" });
      } catch (error) {
        audit("smt_units_unconfirmed", { requestId, errorCode: "UNITS_READ_FAILED" });
      }
      const after = await read("file", "get_fileinfo");
      const beforeRevision = revision(info.revision);
      const afterRevision = revision(after && after.revision);
      if (!after || after.file !== info.file || beforeRevision !== afterRevision) {
        const error = new Error("Model identity changed"); error.code = "MODEL_CHANGED"; throw error;
      }
      const revisionCheck = beforeRevision == null ? "FILE_ONLY_REVISION_UNAVAILABLE" : "FILE_AND_REVISION_MATCH";
      const data = assessment.assess({ model: info.file, revision: beforeRevision, surfaces: surfaces && surfaces.surflist, box, units });
      audit("smt_analyze_http", {
        requestId, model: data.model, revision: data.revision, units: data.units,
        surfaceCount: data.surfaceCount, planeCount: null, nativeConvertReady: false,
        nativeSheetmetalState: data.nativeSheetmetalState,
        thinWallShapeAssessment: data.thinWallShapeAssessment,
        uniformThicknessAssessment: data.uniformThicknessAssessment,
        estimatedThickness: null, suggestedMode: null, reasonCodes: data.reasonCodes,
        revisionCheck, unitReadOutcome
      });
      render(data, revisionCheck);
    } catch (error) {
      const code = error && error.code || (error instanceof core.ApiError ? "CREOSON_READ_FAILED" : "ANALYSIS_FAILED");
      const hints = { NOT_PART: "当前激活模型不是零件", MODEL_CHANGED: "模型或修订变化，请停止核对", INVALID_RESPONSE: "读取响应缺失或格式无效", READ_ONLY_VIOLATION: "拒绝非读取操作", CREOSON_READ_FAILED: "CREOSON读取失败，请保留请求证据", ANALYSIS_FAILED: "读取或显示失败" };
      audit("smt_analyze_failed", { requestId, errorCode: code });
      status("分析失败：" + code + "；" + (hints[code] || hints.ANALYSIS_FAILED), "err");
    } finally {
      busy = false;
      core.setBusy(false);
      syncButtons();
    }
  }

  function downloadAnalysisLog() {
    if (busy) return;
    const events = assessment.analysisEvents(core.events, core.sessionId);
    if (!events.length) return;
    const blob = new Blob([events.map(event => JSON.stringify(event)).join("\n") + "\n"], { type: "application/x-ndjson;charset=utf-8" });
    const url = URL.createObjectURL(blob);
    const link = document.createElement("a");
    link.href = url;
    link.download = "p1_readonly_analysis_" + new Date().toISOString().replace(/[:.]/g, "-") + ".jsonl";
    document.body.appendChild(link);
    link.click();
    link.remove();
    setTimeout(() => URL.revokeObjectURL(url), 10000);
  }

  el("smtAnalyzeBtn").addEventListener("click", analyze);
  el("smtLogBtn").addEventListener("click", downloadAnalysisLog);
  el("smtConvertBtn").addEventListener("click", () => {
    audit("smt_convert_blocked", { errorCode: "CHANNEL_BLOCKED", nativeConvertReady: false });
    status("转换入口未开放；不会从本页面提交模型写操作。", "err");
  });
  const params = new URLSearchParams(location.search);
  if (params.get("guard") === "need_part") {
    blocked = true;
    status("当前入口不是普通零件，分析已阻止。", "err");
  } else if (params.get("state") === "sheetmetal" || params.get("modelMode") === "sheetmetal") {
    blocked = true;
    status("入口报告为钣金模型；本页面不重复分析，也未重新读取原生状态。", "info");
  } else if (params.get("modelMode") !== "solid_part") {
    blocked = true;
    status("模型入口未确认，请从当前零件命令重新打开。", "err");
  }
  audit("smt_mode_guard", { blocked, nativeConvertReady: false });
  syncButtons();
})();
