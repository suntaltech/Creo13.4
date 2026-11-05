"use strict";
/* 盛拓 Creo 工具箱公共层：会话、审计、状态、本地存储、原生桥接和版本工具。
   各业务模块只使用本文件暴露的 SiswCore，不重复实现连接与错误包装。 */
(function () {
  const state = {
    sessionId: "",
    events: [],
    busy: false,
    statusEl: null,
    onAuditChange: null
  };

  class ApiError extends Error {
    constructor(message, uncertain = false) {
      super(message);
      this.name = "ApiError";
      this.uncertain = uncertain;
    }
  }

  function el(id) {
    return document.getElementById(id);
  }

  function bindStatus(element) {
    state.statusEl = element;
  }

  function setStatus(message, kind = "info") {
    if (!state.statusEl) return;
    state.statusEl.textContent = message;
    state.statusEl.className = kind;
  }

  function audit(event, details = {}) {
    // 审计中不保留会话凭据；保留现有事件 API、订阅与下载格式。
    const safeDetails = JSON.parse(JSON.stringify(details, (key, value) => {
      if (/session.?id|token|authorization/i.test(key)) return "[REDACTED]";
      if (typeof value === "string" && state.sessionId) return value.split(state.sessionId).join("[REDACTED]");
      return value;
    }));
    state.events.push({ timestamp: new Date().toISOString(), event, ...safeDetails });
    if (typeof state.onAuditChange === "function") state.onAuditChange(state.events);
  }

  function setBusy(value) {
    state.busy = Boolean(value);
  }

  function request(command, fn, data = {}, id = state.sessionId, timeoutMs = 30000) {
    const controller = new AbortController();
    const timer = setTimeout(() => controller.abort(), timeoutMs);
    return fetch("/creoson", {
      method: "POST",
      signal: controller.signal,
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ sessionId: id, command, function: fn, data })
    }).then(response => {
      if (!response.ok) throw new ApiError(`HTTP ${response.status}`, true);
      return response.json();
    }).then(result => {
      if (!result || !result.status || typeof result.status.error !== "boolean")
        throw new ApiError("CREOSON 返回缺少有效 status", true);
      if (result.status.error) throw new ApiError(result.status.message || "CREOSON 执行错误");
      return result;
    }).catch(error => {
      if (error instanceof ApiError) throw error;
      throw new ApiError(error.name === "AbortError" ? "请求超时" : `通信失败：${error.message}`, true);
    }).finally(() => clearTimeout(timer));
  }

  async function connectSession(timeoutMs = 30000) {
    state.sessionId = "";
    const result = await request("connection", "connect", {}, "", timeoutMs);
    if (typeof result.sessionId !== "string" || !result.sessionId)
      throw new ApiError("连接返回缺少 sessionId");
    state.sessionId = result.sessionId;
  }

  async function ensureSession(options = {}) {
    const startupRetry = options.startupRetry === true;
    const attempts = startupRetry ? 4 : 1;
    const timeoutMs = startupRetry ? 3000 : 30000;
    if (!state.sessionId) {
      // 仅P1显式选择启动重试；默认保持原公共层单次30秒连接行为。
      for (let attempt = 1; attempt <= attempts; attempt++) {
        setStatus(`正在连接 CREOSON（${attempt}/${attempts}），服务可能正在启动…`);
        try {
          await connectSession(timeoutMs);
          audit("connected", { connected: true, attempt });
          setStatus("已连接 CREOSON。", "ok");
          return;
        } catch (error) {
          audit("connect_attempt_failed", { attempt, uncertain: Boolean(error.uncertain) });
          if (!error.uncertain) throw error; // 业务错误不按服务启动失败盲目重试。
          if (attempt === attempts) {
            audit("connect_exhausted", { errorCode: "SERVICE_NOT_READY", attempt, saved: false });
            setStatus("CREOSON 服务未就绪，请检查 p1_launch_stderr.log、jlink.log 和9056端口占用。", "err");
            throw new ApiError("CREOSON 服务未就绪，请检查 p1_launch_stderr.log、jlink.log 和9056端口占用", true);
          }
          await new Promise(resolve => setTimeout(resolve, 1000));
        }
      }
    }
  }

  async function call(command, fn, data = {}, timeoutMs = 30000) {
    await ensureSession();
    try {
      return (await request(command, fn, data, state.sessionId, timeoutMs)).data;
    } catch (error) {
      if (!state.sessionId || !error.uncertain) throw error;
      state.sessionId = "";
      await ensureSession();
      return (await request(command, fn, data, state.sessionId, timeoutMs)).data;
    }
  }

  const storage = {
    get(key) {
      try { return localStorage.getItem(key); } catch (_) { return null; }
    },
    set(key, value) {
      try { localStorage.setItem(key, value); } catch (_) { }
    }
  };

  function downloadLog(prefix) {
    if (!state.events.length || state.busy) return;
    const blob = new Blob([state.events.map(event => JSON.stringify(event)).join("\n") + "\n"],
      { type: "application/x-ndjson;charset=utf-8" });
    const url = URL.createObjectURL(blob);
    const link = document.createElement("a");
    link.href = url;
    link.download = `${prefix}_${new Date().toISOString().replace(/[:.]/g, "-")}.jsonl`;
    document.body.appendChild(link);
    link.click();
    link.remove();
    setTimeout(() => URL.revokeObjectURL(url), 10000);
  }

  function bridgeCall() {
    // 保留API形状，但删除所有未验收的原生分支；任何key/action均不能访问window.external。
    audit("native_bridge_blocked", { errorCode: "CHANNEL_BLOCKED", saved: false });
    return Promise.reject(new ApiError("CHANNEL_BLOCKED：当前原生浏览器桥接全部关闭"));
  }

  function parseVersion(text) {
    const parts = String(text || "").trim().split(/[^0-9A-Za-z]+/).filter(Boolean);
    const numbers = parts.map(part => {
      const value = Number.parseInt(part, 10);
      return Number.isNaN(value) ? 0 : value;
    });
    return numbers.length ? numbers : [0];
  }

  function compareVersions(remote, local) {
    const left = parseVersion(remote);
    const right = parseVersion(local);
    const length = Math.max(left.length, right.length);
    for (let i = 0; i < length; i++) {
      const a = left[i] || 0;
      const b = right[i] || 0;
      if (a !== b) return a > b ? 1 : -1;
    }
    return 0;
  }

  async function loadLocalVersion(labelEl, fallbackVersion) {
    let localVersion = fallbackVersion || "0.0.0";
    try {
      const response = await fetch("/version.json", { cache: "no-store" });
      if (response.ok) {
        const manifest = await response.json();
        if (manifest && typeof manifest.version === "string") localVersion = manifest.version;
      }
    } catch (_) { }
    if (labelEl) labelEl.textContent = `当前版本 v${localVersion}`;
    return localVersion;
  }

  window.SiswCore = {
    ApiError,
    audit,
    bindStatus,
    setStatus,
    setBusy,
    call,
    ensureSession,
    connectSession,
    request,
    el,
    storage,
    downloadLog,
    bridgeCall,
    parseVersion,
    compareVersions,
    loadLocalVersion,
    debugMode: new URLSearchParams(location.search).get("debug") === "1",
    get busy() { return state.busy; },
    get events() { return state.events; },
    set onAuditChange(handler) { state.onAuditChange = handler; },
    get sessionId() { return state.sessionId; }
  };
})();
