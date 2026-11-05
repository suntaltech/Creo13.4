"use strict";
/* 零件工具：当前激活零件钣金适用性只读分析。
   说明：Creo 13.4 同步 J-Link 的 AddBrowserMessageListener 会长期占用 CIP 原生通道，
   因此本版本不通过原生桥接执行破坏性转换；分析数据走 CREOSON HTTP，转换按钮保持禁用待安全通道验收。 */
(function () {
  const core = window.SiswCore;
  const el = core.el;
  const audit = core.audit;
  core.bindStatus(el("smtStatus"));

  const nativeConvertReady = false;
  let analysisSequence = 0;
  let smtAnalysis = null;
  let busy = false;
  let blocked = false;

  function syncButtons() {
    el("smtAnalyzeBtn").disabled = busy || blocked;
    el("smtConvertBtn").disabled = true;
    document.querySelectorAll("input[name='smtMode']").forEach(radio => radio.disabled = true);
    el("smtThickness").disabled = true;
    el("smtSave").disabled = true;
    el("smtSave").checked = false;
  }

  function setBusy(value) {
    busy = value;
    core.setBusy(value);
    syncButtons();
  }

  function setSmtStatus(message, kind = "info") {
    el("smtStatus").textContent = message;
    el("smtStatus").className = kind;
  }

  function applyModeBlock() {
    const params = new URLSearchParams(location.search);
    if (params.get("guard") === "need_part") {
      blocked = true;
      setSmtStatus("当前不是零件 .prt；实体转钣金分析只在普通零件界面可用，装配和工程图请使用对应功能。", "err");
      el("smtResult").textContent = "模式不匹配：本页面不会执行转换。";
    } else if (params.get("state") === "sheetmetal" || params.get("modelMode") === "sheetmetal") {
      blocked = true;
      smtAnalysis = { sheetmetal: true, largestPlane: null };
      setSmtStatus("当前零件已是钣金，无需重复转换；本页面仅作状态提示，转换按钮保持禁用。", "ok");
      el("smtResult").textContent = "状态：已是钣金零件。";
    } else if (params.get("modelMode") !== "solid_part") {
      blocked = true;
      setSmtStatus("模型状态未确认或不支持；请从当前普通零件命令重新打开本页面。", "err");
    }
    audit("smt_mode_guard", { blocked, nativeConvertReady, saved: false });
    syncButtons();
  }

  function selectedSmtMode() {
    const selected = document.querySelector("input[name='smtMode']:checked");
    return selected ? selected.value : "driving";
  }

  function currentDesiredThickness() {
    const raw = el("smtThickness").value.trim();
    if (!/^(?:[0-9]+(?:\.[0-9]*)?|\.[0-9]+)(?:[eE][+-]?[0-9]+)?$/.test(raw)) return null;
    const value = Number(raw);
    return Number.isFinite(value) && value >= 0.1 && value <= 20 ? value : null;
  }

  function formatLength(value, unit) {
    return typeof value === "number" && Number.isFinite(value) && value > 0 ? `${value.toFixed(3)} ${unit}` : "未取得";
  }

  function formatArea(value, unit) {
    return typeof value === "number" && Number.isFinite(value) && value > 0 ? `${value.toFixed(3)} ${unit}²` : "未识别";
  }

  function formatThickness(value) {
    return typeof value === "number" && Number.isFinite(value) && value > 0 ? `${value.toFixed(3)} mm` : "未指定";
  }

  function surfaceSpan(surface, axis) {
    const min = surface?.min_extent?.[axis];
    const max = surface?.max_extent?.[axis];
    if (typeof min !== "number" || typeof max !== "number") return NaN;
    return Math.abs(max - min);
  }

  function isEnvelopeCandidate(surface) {
    const tolerance = 0.01;
    return ["x", "y", "z"].some(axis => {
      const span = surfaceSpan(surface, axis);
      return Number.isFinite(span) && span <= tolerance;
    });
  }

  function setThicknessUnit(modelUnits) {
    // 输入契约仍为0.1–20 mm；模型单位只展示，不将cm/inch数值冒充mm或静默换算。
    el("smtThicknessUnit").textContent = modelUnits
      ? `输入契约按 mm；模型长度单位：${modelUnits}（未换算，参数禁用）`
      : "单位待确认（模型单位未确认；板厚参数禁用）";
  }

  function renderSmtAnalysis(data) {
    smtAnalysis = data;
    const plane = data.largestPlane;
    const spans = data.spans || {};
    const unit = data.units || "单位未确认";
    setThicknessUnit(data.units);
    const lines = [
      `模型：${data.model || "未知"}`,
      `当前状态：${data.sheetmetal ? "已是钣金零件" : "普通零件入口；IsSheetmetal / 实体数需原生确认"}`,
      `revision：${data.revision ?? "未知"}；长度单位：${unit}`,
      `曲面数量：${data.surfaceCount ?? "未知"}；启发式包络候选数：${data.planeCount ?? "未知"}（不是官方平面类型）`,
      `最大启发式候选（非真实面选择）：${plane ? `曲面 ID ${plane.id}，面积 ${formatArea(plane.area, unit)}` : "未找到启发式候选（不能据此判断无真实平面）"}`,
      `包围尺寸：X ${formatLength(spans.x, unit)}；Y ${formatLength(spans.y, unit)}；Z ${formatLength(spans.z, unit)}`,
      `最小包络跨度（非正式壁厚）：${formatLength(data.estimatedThickness, unit)}`,
      `指定转换板厚：${formatThickness(currentDesiredThickness())}（仅本地参数记忆位，未提交、未换算；通道关闭）`,
      "建议人工方式：等厚薄板可评估 driving，块状实体可评估 shell；不能根据包络尺寸自动决定。"
    ];
    if (!data.sheetmetal) {
      lines.push("注意：当前版本只做只读分析；自动转换按钮保持禁用，避免阻塞 Creo 13.4 同步会话。");
    }
    el("smtResult").textContent = lines.join("\n");
    if (data.sheetmetal) {
      setSmtStatus("当前零件已经是钣金，无需重复转换。", "ok");
    } else if (!plane) {
      setSmtStatus("分析完成，未找到包络启发式候选；真实平面仍需原生确认。", "err");
    } else {
      setSmtStatus("只读分析完成；自动转换待安全原生通道验收，当前不会修改模型。", "ok");
    }
    syncButtons();
  }

  async function smtAnalyze() {
    if (busy || blocked) return;
    setBusy(true);
    smtAnalysis = null;
    setThicknessUnit(null);
    el("smtResult").textContent = "";
    setSmtStatus("正在通过 CREOSON 读取当前零件几何…", "info");
    const requestId = `smt-analysis-${Date.now()}-${++analysisSequence}`;
    audit("smt_analysis_started", { requestId, saved: false });
    const read = async (command, fn, data = {}) => {
      if (!["file/get_fileinfo", "feature/list", "geometry/get_surfaces", "geometry/bound_box", "file/get_length_units"].includes(`${command}/${fn}`)) {
        throw new Error("READ_ONLY：拒绝非只读请求");
      }
      audit("smt_read_started", { requestId, command, function: fn, saved: false });
      try {
        const dataResult = await core.call(command, fn, data, 60000);
        audit("smt_read_passed", { requestId, command, function: fn, saved: false });
        return dataResult;
      } catch (error) {
        audit("smt_read_failed", { requestId, command, function: fn, saved: false });
        throw error;
      }
    };
    try {
      await core.ensureSession({ startupRetry: true });
      const info = await read("file", "get_fileinfo");
      if (!info || typeof info.file !== "string" || !/\.prt$/i.test(info.file)) {
        throw new Error("当前激活模型不是 .prt 零件；请先打开普通零件");
      }

      const features = await read("feature", "list", { file: info.file, status: "ACTIVE", no_comp: true, paths: true, inc_unnamed: true });
      const surfaces = await read("geometry", "get_surfaces", { file: info.file });
      const box = await read("geometry", "bound_box", { file: info.file });
      let units = null;
      try {
        const lengthUnits = await read("file", "get_length_units", { file: info.file });
        if (typeof lengthUnits?.units !== "string" || !lengthUnits.units.trim()) throw new Error("单位响应无效");
        units = lengthUnits.units.trim();
      } catch (_) {
        audit("smt_units_unconfirmed", { requestId, errorCode: "UNITS_UNCONFIRMED", saved: false });
      }
      const after = await read("file", "get_fileinfo");
      if (after?.file !== info.file || after?.revision !== info.revision) throw new Error("MODEL_CHANGED：分析期间模型或修订已变化，请重新分析");
      if (!Array.isArray(features?.featlist) || !Array.isArray(surfaces?.surflist)) throw new Error("INVALID_RESPONSE：特征或曲面列表格式错误");
      for (const axis of ["x", "y", "z"]) {
        if (!Number.isFinite(box?.[axis + "min"]) || !Number.isFinite(box?.[axis + "max"]) || box[axis + "max"] < box[axis + "min"]) {
          throw new Error("INVALID_RESPONSE：模型包络范围无效");
        }
      }
      // 特征名不证明 IsSheetmetal；包络候选不证明 SurfaceType/可见性。
      const sheetmetal = null;

      const surfaceList = Array.isArray(surfaces?.surflist) ? surfaces.surflist : [];
      const candidates = surfaceList.filter(isEnvelopeCandidate)
        .sort((left, right) => (right.area || 0) - (left.area || 0));
      const largest = candidates[0] || null;

      const spans = {
        x: Number(box?.xmax) - Number(box?.xmin),
        y: Number(box?.ymax) - Number(box?.ymin),
        z: Number(box?.zmax) - Number(box?.zmin)
      };
      const minSpan = Math.min(
        Math.max(0, Number.isFinite(spans.x) ? spans.x : 0),
        Math.max(0, Number.isFinite(spans.y) ? spans.y : 0),
        Math.max(0, Number.isFinite(spans.z) ? spans.z : 0)
      );
      const estimatedThickness = Number.isFinite(minSpan) && minSpan > 0 ? minSpan : null;

      const data = {
        model: info.file,
        revision: info.revision,
        units,
        nativeConvertReady,
        saved: false,
        sheetmetal,
        surfaceCount: surfaceList.length,
        planeCount: candidates.length,
        estimatedThickness,
        suggestedMode: null,
        largestPlane: largest ? { id: largest.surface_id, area: largest.area } : null,
        spans
      };
      audit("smt_analyze_http", {
        requestId,
        units: data.units,
        nativeConvertReady,
        saved: false,
        model: data.model,
        sheetmetal: data.sheetmetal,
        surfaceCount: data.surfaceCount,
        planeCount: data.planeCount
      });
      renderSmtAnalysis(data);
    } catch (error) {
      const message = String(error.message || "未知错误");
      const code = message.startsWith("MODEL_CHANGED") ? "MODEL_CHANGED"
        : message.startsWith("INVALID_RESPONSE") ? "INVALID_RESPONSE"
        : message.startsWith("当前激活模型") ? "NOT_PART"
        : message === "请求超时" ? "TIMEOUT"
        : error instanceof core.ApiError ? (error.uncertain ? "NETWORK_ERROR" : "CREOSON_ERROR") : "ANALYSIS_FAILED";
      const hints = { MODEL_CHANGED: "分析期间激活模型或修订发生变化，请重新分析", INVALID_RESPONSE: "只读接口响应格式无效",
        NOT_PART: "当前激活模型不是 .prt 普通零件", TIMEOUT: "只读请求超时，请检查 CREOSON",
        NETWORK_ERROR: "CREOSON 服务未就绪或通信失败，请检查 p1_launch_stderr.log、jlink.log 和9056端口占用", CREOSON_ERROR: "CREOSON 业务请求失败，请检查当前模型",
        ANALYSIS_FAILED: "只读分析失败，请检查连接和当前模型" };
      audit("smt_analyze_failed", { requestId, errorCode: code, saved: false });
      setSmtStatus(`分析失败：${code}：${hints[code]}`, "err");
    } finally {
      setBusy(false);
    }
  }

  async function smtConvert() {
    if (busy || blocked) return;

    setBusy(true);
    try {
      audit("smt_convert_blocked", {
        model: smtAnalysis?.model || "",
        errorCode: "CHANNEL_BLOCKED",
        nativeConvertReady,
        saved: false,
        mode: selectedSmtMode(),
        desiredThickness: currentDesiredThickness(),
        reason: "safe native conversion channel pending validation; no model modification"
      });
      setSmtStatus("指定板厚转换暂未开放：安全原生通道验收前不会修改模型；当前只保留板厚参数位和只读分析。", "err");
    } finally {
      setBusy(false);
    }
  }

  function initStoredOptions() {
    const savedMode = core.storage.get("smt_mode");
    if (savedMode === "driving" || savedMode === "shell") {
      document.querySelector(`input[name='smtMode'][value='${savedMode}']`).checked = true;
    }
    el("smtThickness").value = core.storage.get("smt_thickness") || "";
    if (currentDesiredThickness() === null) el("smtThickness").value = "";
    el("smtSave").checked = false;
    core.storage.set("smt_save", "0");
  }

  document.querySelectorAll("input[name='smtMode']").forEach(radio => {
    radio.addEventListener("change", () => {
      smtAnalysis = null;
      syncButtons();
      core.storage.set("smt_mode", radio.value);
    });
  });
  el("smtThickness").addEventListener("change", () => {
    core.storage.set("smt_thickness", currentDesiredThickness() === null ? "" : String(currentDesiredThickness()));
  });
  el("smtSave").addEventListener("change", () => {
    el("smtSave").checked = false;
    core.storage.set("smt_save", "0");
  });
  el("smtAnalyzeBtn").addEventListener("click", smtAnalyze);
  el("smtConvertBtn").addEventListener("click", smtConvert);

  setThicknessUnit(null);
  initStoredOptions();
  applyModeBlock();
  syncButtons();
})();
