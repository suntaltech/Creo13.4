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
    state.events.push({ timestamp: new Date().toISOString(), event, ...details });
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

  async function connectSession() {
    state.sessionId = "";
    const result = await request("connection", "connect", {}, "");
    if (typeof result.sessionId !== "string" || !result.sessionId)
      throw new ApiError("连接返回缺少 sessionId");
    state.sessionId = result.sessionId;
  }

  async function ensureSession() {
    if (!state.sessionId) {
      setStatus("正在自动连接 CREOSON…");
      await connectSession();
      audit("connected", { sessionId: state.sessionId });
      setStatus("已连接，请生成预览。", "ok");
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

  function bridgeCall(options) {
    const bridgeKey = options.bridgeKey || "sisw-smt";
    const replyName = options.replyName || "__smtBridgeReply";
    const action = options.action || "";
    const params = options.params || {};
    const timeoutMs = options.timeoutMs || 15000;
    const timeoutMessage = options.timeoutMessage || `原生桥接超时：${timeoutMs / 1000} 秒内未收到 Creo 回调`;

    return new Promise((resolve, reject) => {
      if (!window.external || typeof window.external.ptc !== "function") {
        reject(new ApiError("本功能需要在 Creo 内嵌浏览器中使用，请通过插件按钮打开本页面。"));
        return;
      }
      const previous = window[replyName];
      if (previous && typeof previous.pending === "object" && previous.pending) {
        previous.pending.reject(new ApiError("上一次原生操作被新操作打断"));
      }
      const pending = { resolve, reject };
      window[replyName] = payload => {
        const current = pending;
        window[replyName] = null;
        if (!current) return;
        if (payload && payload.ok) current.resolve(payload.data);
        else current.reject(new ApiError((payload && payload.error) || "原生操作失败"));
      };
      window[replyName].pending = pending;

      const query = new URLSearchParams({ action, ...params }).toString();
      try {
        window.external.ptc(`ToolkitJSBridge=${bridgeKey}?${query}`);
      } catch (error) {
        window[replyName] = null;
        reject(error instanceof Error ? error : new ApiError(String(error)));
        return;
      }
      setTimeout(() => {
        if (window[replyName] && window[replyName].pending === pending) {
          window[replyName] = null;
          pending.reject(new ApiError(timeoutMessage));
        }
      }, timeoutMs);
    });
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
