package com.example.browser

object InjectedScripts {

    val CONSOLE_OVERRIDE_SCRIPT = """
        (function() {
            if (window.__devToolsConsoleInjected) return;
            window.__devToolsConsoleInjected = true;

            var origLog = console.log, origWarn = console.warn, origError = console.error, origInfo = console.info;

            function formatArg(arg) {
                if (arg === null) return "null";
                if (arg === undefined) return "undefined";
                if (typeof arg === 'object') {
                    try { return JSON.stringify(arg); } catch(e) { return String(arg); }
                }
                return String(arg);
            }

            function sendConsole(level, args) {
                try {
                    var str = Array.prototype.slice.call(args).map(formatArg).join(" ");
                    if (window.AndroidDevTools && window.AndroidDevTools.onConsoleLog) {
                        window.AndroidDevTools.onConsoleLog(level, str, window.location.href);
                    }
                } catch(e) {}
            }

            console.log = function() { sendConsole("LOG", arguments); if(origLog) origLog.apply(console, arguments); };
            console.warn = function() { sendConsole("WARN", arguments); if(origWarn) origWarn.apply(console, arguments); };
            console.error = function() { sendConsole("ERROR", arguments); if(origError) origError.apply(console, arguments); };
            console.info = function() { sendConsole("INFO", arguments); if(origInfo) origInfo.apply(console, arguments); };

            window.addEventListener('error', function(e) {
                var msg = (e.message || 'Script error') + ' at ' + (e.filename || '') + ':' + (e.lineno || 0);
                if (window.AndroidDevTools && window.AndroidDevTools.onConsoleLog) {
                    window.AndroidDevTools.onConsoleLog("ERROR", msg, window.location.href);
                }
            });

            window.addEventListener('unhandledrejection', function(e) {
                var msg = 'Unhandled Promise Rejection: ' + (e.reason ? (e.reason.message || String(e.reason)) : 'unknown');
                if (window.AndroidDevTools && window.AndroidDevTools.onConsoleLog) {
                    window.AndroidDevTools.onConsoleLog("ERROR", msg, window.location.href);
                }
            });
        })();
    """.trimIndent()

    val NETWORK_OVERRIDE_SCRIPT = """
        (function() {
            if (window.__devToolsNetInjected) return;
            window.__devToolsNetInjected = true;

            function sendNetJson(data) {
                try {
                    if (window.AndroidDevTools && window.AndroidDevTools.onNetworkRequestJson) {
                        window.AndroidDevTools.onNetworkRequestJson(JSON.stringify(data));
                    }
                } catch(e) {}
            }

            function parseHeaders(headersObj) {
                var res = {};
                if (!headersObj) return res;
                if (typeof headersObj.forEach === 'function') {
                    try { headersObj.forEach(function(val, key) { res[key] = val; }); } catch(e) {}
                } else if (typeof headersObj === 'object') {
                    for (var k in headersObj) {
                        try { res[k] = String(headersObj[k]); } catch(e) {}
                    }
                }
                return res;
            }

            // 1. Fetch Interceptor
            var origFetch = window.fetch;
            if (origFetch) {
                window.fetch = function() {
                    var args = arguments;
                    var input = args[0];
                    var init = args[1] || {};
                    var url = typeof input === 'string' ? input : (input && input.url ? input.url : 'Fetch');
                    var method = (init.method || (input && input.method) || 'GET').toUpperCase();
                    var reqBody = init.body ? String(init.body) : '';
                    var reqHeaders = parseHeaders(init.headers || (input && input.headers));
                    var startTime = Date.now();

                    return origFetch.apply(this, args).then(function(response) {
                        var duration = Date.now() - startTime;
                        var status = response.status;
                        var statusText = response.statusText || (status === 200 ? 'OK' : 'HTTP ' + status);
                        var clone = response.clone();
                        var resHeaders = parseHeaders(clone.headers);

                        clone.text().then(function(bodyText) {
                            sendNetJson({
                                url: url,
                                method: method,
                                statusCode: status,
                                statusText: statusText,
                                type: 'fetch',
                                durationMs: duration,
                                sizeBytes: bodyText ? bodyText.length : 0,
                                initiator: 'fetch()',
                                requestHeaders: reqHeaders,
                                requestBody: reqBody,
                                responseHeaders: resHeaders,
                                responseBody: bodyText.substring(0, 100000)
                            });
                        }).catch(function() {
                            sendNetJson({
                                url: url,
                                method: method,
                                statusCode: status,
                                statusText: statusText,
                                type: 'fetch',
                                durationMs: duration,
                                sizeBytes: 0,
                                initiator: 'fetch()',
                                requestHeaders: reqHeaders,
                                requestBody: reqBody,
                                responseHeaders: resHeaders,
                                responseBody: '[Binary or Stream response]'
                            });
                        });

                        return response;
                    }).catch(function(err) {
                        var duration = Date.now() - startTime;
                        sendNetJson({
                            url: url,
                            method: method,
                            statusCode: 0,
                            statusText: 'Failed',
                            type: 'fetch',
                            durationMs: duration,
                            sizeBytes: 0,
                            initiator: 'fetch()',
                            requestHeaders: reqHeaders,
                            requestBody: reqBody,
                            responseHeaders: {},
                            responseBody: 'Fetch Error: ' + (err.message || 'Network Failed')
                        });
                        throw err;
                    });
                };
            }

            // 2. XMLHttpRequest Interceptor
            var origOpen = XMLHttpRequest.prototype.open;
            var origSetHeader = XMLHttpRequest.prototype.setRequestHeader;
            var origSend = XMLHttpRequest.prototype.send;

            XMLHttpRequest.prototype.open = function(method, url) {
                this._url = url;
                this._method = (method || 'GET').toUpperCase();
                this._startTime = Date.now();
                this._reqHeaders = {};
                return origOpen.apply(this, arguments);
            };

            XMLHttpRequest.prototype.setRequestHeader = function(header, value) {
                if (!this._reqHeaders) this._reqHeaders = {};
                this._reqHeaders[header] = value;
                return origSetHeader.apply(this, arguments);
            };

            XMLHttpRequest.prototype.send = function(body) {
                var xhr = this;
                var reqBodyStr = body ? String(body) : '';

                this.addEventListener('load', function() {
                    var duration = Date.now() - (xhr._startTime || Date.now());
                    var rawResHeaders = xhr.getAllResponseHeaders() || '';
                    var parsedResHeaders = {};
                    rawResHeaders.split('\r\n').forEach(function(line) {
                        var parts = line.split(': ');
                        if (parts.length > 1) parsedResHeaders[parts[0].trim()] = parts.slice(1).join(': ').trim();
                    });

                    sendNetJson({
                        url: xhr._url || 'XHR',
                        method: xhr._method || 'GET',
                        statusCode: xhr.status,
                        statusText: xhr.statusText || (xhr.status === 200 ? 'OK' : 'XHR ' + xhr.status),
                        type: 'xhr',
                        durationMs: duration,
                        sizeBytes: (xhr.responseText || '').length,
                        initiator: 'XMLHttpRequest',
                        requestHeaders: xhr._reqHeaders || {},
                        requestBody: reqBodyStr,
                        responseHeaders: parsedResHeaders,
                        responseBody: (xhr.responseText || '').substring(0, 100000)
                    });
                });

                this.addEventListener('error', function() {
                    sendNetJson({
                        url: xhr._url || 'XHR',
                        method: xhr._method || 'GET',
                        statusCode: 0,
                        statusText: 'Failed',
                        type: 'xhr',
                        durationMs: Date.now() - (xhr._startTime || Date.now()),
                        sizeBytes: 0,
                        initiator: 'XMLHttpRequest',
                        requestHeaders: xhr._reqHeaders || {},
                        requestBody: reqBodyStr,
                        responseHeaders: {},
                        responseBody: 'XHR Network Error'
                    });
                });

                return origSend.apply(this, arguments);
            };

            // 3. WebSocket Interceptor
            var OrigWebSocket = window.WebSocket;
            if (OrigWebSocket) {
                window.WebSocket = function(url, protocols) {
                    var ws = protocols ? new OrigWebSocket(url, protocols) : new OrigWebSocket(url);
                    var fullUrl = typeof url === 'string' ? url : String(url);

                    sendNetJson({
                        url: fullUrl,
                        method: 'GET',
                        statusCode: 101,
                        statusText: 'Switching Protocols',
                        type: 'ws',
                        durationMs: 0,
                        sizeBytes: 0,
                        initiator: 'WebSocket',
                        requestHeaders: { 'Upgrade': 'websocket', 'Connection': 'Upgrade' },
                        requestBody: '',
                        responseHeaders: { 'HTTP/1.1': '101 Switching Protocols' },
                        responseBody: 'WebSocket Connection Established'
                    });

                    var origWsSend = ws.send;
                    ws.send = function(data) {
                        try {
                            if (window.AndroidDevTools && window.AndroidDevTools.onWebSocketFrame) {
                                window.AndroidDevTools.onWebSocketFrame(fullUrl, 'sent', String(data));
                            }
                        } catch(e) {}
                        return origWsSend.apply(this, arguments);
                    };

                    ws.addEventListener('message', function(evt) {
                        try {
                            if (window.AndroidDevTools && window.AndroidDevTools.onWebSocketFrame) {
                                window.AndroidDevTools.onWebSocketFrame(fullUrl, 'received', String(evt.data));
                            }
                        } catch(e) {}
                    });

                    return ws;
                };
                window.WebSocket.prototype = OrigWebSocket.prototype;
            }

            // 4. PerformanceObserver for Subresources (JS, CSS, Image, Media, Font, Doc)
            try {
                if (window.PerformanceObserver) {
                    var reportedUrls = {};
                    var observer = new PerformanceObserver(function(list) {
                        var entries = list.getEntries();
                        for (var i = 0; i < entries.length; i++) {
                            var entry = entries[i];
                            var entryUrl = entry.name;
                            if (!entryUrl || entryUrl.startsWith('data:') || reportedUrls[entryUrl]) continue;

                            var initType = entry.initiatorType || 'other';
                            if (initType === 'fetch' || initType === 'xmlhttprequest') continue; // Already intercepted

                            reportedUrls[entryUrl] = true;

                            var category = 'other';
                            if (initType === 'script' || entryUrl.endsWith('.js')) category = 'js';
                            else if (initType === 'css' || initType === 'link' || entryUrl.endsWith('.css')) category = 'css';
                            else if (initType === 'img' || entryUrl.match(/\.(png|jpg|jpeg|gif|svg|webp|ico)/i)) category = 'img';
                            else if (initType === 'media' || initType === 'video' || initType === 'audio' || entryUrl.match(/\.(mp4|webm|mp3|ogg|wav|m3u8|ts)/i)) category = 'media';
                            else if (initType === 'font' || entryUrl.match(/\.(woff|woff2|ttf|otf|eot)/i)) category = 'font';
                            else if (initType === 'iframe' || initType === 'navigation') category = 'doc';

                            sendNetJson({
                                url: entryUrl,
                                method: 'GET',
                                statusCode: 200,
                                statusText: 'OK',
                                type: category,
                                durationMs: Math.round(entry.duration || 0),
                                sizeBytes: entry.transferSize || entry.encodedBodySize || 0,
                                initiator: initType,
                                requestHeaders: { 'Accept': '*/*' },
                                requestBody: '',
                                responseHeaders: { 'Content-Type': initType },
                                responseBody: '[Resource loaded by browser]'
                            });
                        }
                    });
                    observer.observe({ entryTypes: ['resource'] });
                }
            } catch(e) {}

            // 5. Media elements (<video>, <audio>) detection
            function observeMediaElements() {
                var mediaEls = document.querySelectorAll('video, audio, source');
                for (var j = 0; j < mediaEls.length; j++) {
                    var m = mediaEls[j];
                    var mediaUrl = m.src || m.currentSrc;
                    if (mediaUrl && !mediaUrl.startsWith('data:')) {
                        sendNetJson({
                            url: mediaUrl,
                            method: 'GET',
                            statusCode: 200,
                            statusText: 'OK (Media)',
                            type: 'media',
                            durationMs: 0,
                            sizeBytes: 0,
                            initiator: m.tagName.toLowerCase(),
                            requestHeaders: { 'Range': 'bytes=0-' },
                            requestBody: '',
                            responseHeaders: { 'Accept-Ranges': 'bytes' },
                            responseBody: '[Media Stream / Source]'
                        });
                    }
                }
            }
            document.addEventListener('DOMContentLoaded', observeMediaElements);
            setTimeout(observeMediaElements, 2000);
        })();
    """.trimIndent()

    fun getInspectorScript(enable: Boolean): String {
        return if (enable) {
            """
            (function() {
                if (window.__devToolsInspectorActive) return;
                window.__devToolsInspectorActive = true;

                var highlightBox = document.getElementById('__devtools_highlight_box');
                if (!highlightBox) {
                    highlightBox = document.createElement('div');
                    highlightBox.id = '__devtools_highlight_box';
                    highlightBox.style.position = 'fixed';
                    highlightBox.style.pointerEvents = 'none';
                    highlightBox.style.border = '2px solid #38BDF8';
                    highlightBox.style.backgroundColor = 'rgba(56, 189, 248, 0.2)';
                    highlightBox.style.zIndex = '9999999';
                    highlightBox.style.transition = 'all 0.05s ease-out';
                    highlightBox.style.borderRadius = '2px';
                    highlightBox.style.boxShadow = '0 0 8px rgba(56, 189, 248, 0.6)';
                    document.body.appendChild(highlightBox);
                } else {
                    highlightBox.style.display = 'block';
                }

                function onHover(e) {
                    var el = e.target;
                    if (el === highlightBox) return;
                    var rect = el.getBoundingClientRect();
                    highlightBox.style.top = rect.top + 'px';
                    highlightBox.style.left = rect.left + 'px';
                    highlightBox.style.width = rect.width + 'px';
                    highlightBox.style.height = rect.height + 'px';
                }

                function getSelectorPath(element) {
                    if (!element || element.nodeType !== 1) return "";
                    var path = [];
                    var curr = element;
                    while (curr && curr.nodeType === 1) {
                        var tag = curr.tagName.toLowerCase();
                        if (curr.id) {
                            tag += '#' + curr.id;
                            path.unshift(tag);
                            break;
                        } else {
                            var sib = curr, nth = 1;
                            while (sib = sib.previousElementSibling) {
                                if (sib.tagName === curr.tagName) nth++;
                            }
                            tag += ":nth-of-type(" + nth + ")";
                        }
                        path.unshift(tag);
                        curr = curr.parentElement;
                    }
                    return path.join(" > ");
                }

                function onClick(e) {
                    e.preventDefault();
                    e.stopPropagation();
                    var el = e.target;
                    if (el === highlightBox) return;

                    var rect = el.getBoundingClientRect();
                    var computed = window.getComputedStyle(el);
                    var styleMap = {
                        "color": computed.color,
                        "background-color": computed.backgroundColor,
                        "font-family": computed.fontFamily,
                        "font-size": computed.fontSize,
                        "font-weight": computed.fontWeight,
                        "display": computed.display,
                        "position": computed.position,
                        "margin": computed.margin,
                        "padding": computed.padding,
                        "width": Math.round(rect.width) + "px",
                        "height": Math.round(rect.height) + "px"
                    };

                    var attrMap = {};
                    for (var i = 0; i < el.attributes.length; i++) {
                        var attr = el.attributes[i];
                        attrMap[attr.name] = attr.value;
                    }

                    var data = {
                        tagName: el.tagName.toLowerCase(),
                        id: el.id || "",
                        className: el.className || "",
                        attributes: attrMap,
                        computedStyles: styleMap,
                        outerHtml: el.outerHTML ? el.outerHTML.substring(0, 10000) : "",
                        innerHtml: el.innerHTML ? el.innerHTML.substring(0, 3000) : "",
                        selectorPath: getSelectorPath(el),
                        width: Math.round(rect.width),
                        height: Math.round(rect.height),
                        top: Math.round(rect.top),
                        left: Math.round(rect.left)
                    };

                    if (window.AndroidDevTools && window.AndroidDevTools.onElementInspected) {
                        window.AndroidDevTools.onElementInspected(JSON.stringify(data));
                    }
                }

                document.addEventListener('mouseover', onHover, true);
                document.addEventListener('click', onClick, true);

                window.__disableDevToolsInspector = function() {
                    window.__devToolsInspectorActive = false;
                    document.removeEventListener('mouseover', onHover, true);
                    document.removeEventListener('click', onClick, true);
                    if (highlightBox) highlightBox.style.display = 'none';
                };
            })();
            """.trimIndent()
        } else {
            """
            (function() {
                if (window.__disableDevToolsInspector) {
                    window.__disableDevToolsInspector();
                }
            })();
            """.trimIndent()
        }
    }

    val EXTRACT_DOM_TREE_JS = """
        (function() {
            function getSelectorPath(element) {
                if (!element || element.nodeType !== 1) return "";
                var path = [];
                var curr = element;
                while (curr && curr.nodeType === 1) {
                    var tag = curr.tagName.toLowerCase();
                    if (curr.id) {
                        tag += '#' + curr.id;
                        path.unshift(tag);
                        break;
                    } else {
                        var sib = curr, nth = 1;
                        while (sib = sib.previousElementSibling) {
                            if (sib.tagName === curr.tagName) nth++;
                        }
                        tag += ":nth-of-type(" + nth + ")";
                    }
                    path.unshift(tag);
                    curr = curr.parentElement;
                }
                return path.join(" > ");
            }

            function parseNode(node, depth, maxDepth) {
                if (!node || depth > maxDepth) return null;

                if (node.nodeType === 3) { // Text node
                    var text = (node.textContent || '').trim();
                    if (!text) return null;
                    return {
                        nodeId: "text_" + Math.random().toString(36).substr(2, 6),
                        tagName: "#text",
                        isTextNode: true,
                        textContent: text.length > 300 ? text.substring(0, 300) + '...' : text,
                        attributes: {},
                        children: [],
                        selectorPath: ""
                    };
                }

                if (node.nodeType !== 1) return null; // Element nodes only

                var tag = node.tagName.toLowerCase();
                var attrs = {};
                if (node.attributes) {
                    for (var i = 0; i < node.attributes.length; i++) {
                        var a = node.attributes[i];
                        attrs[a.name] = a.value;
                    }
                }

                var childrenList = [];
                var childCount = 0;

                if (node.shadowRoot) {
                    var shadowChild = node.shadowRoot.firstChild;
                    while (shadowChild && childCount < 300) {
                        var pShadow = parseNode(shadowChild, depth + 1, maxDepth);
                        if (pShadow) {
                            childrenList.push(pShadow);
                            childCount++;
                        }
                        shadowChild = shadowChild.nextSibling;
                    }
                }

                var child = node.firstChild;
                while (child && childCount < 300) {
                    var parsed = parseNode(child, depth + 1, maxDepth);
                    if (parsed) {
                        childrenList.push(parsed);
                        childCount++;
                    }
                    child = child.nextSibling;
                }

                return {
                    nodeId: "el_" + tag + "_" + Math.random().toString(36).substr(2, 6),
                    tagName: tag,
                    isTextNode: false,
                    textContent: "",
                    attributes: attrs,
                    children: childrenList,
                    selectorPath: getSelectorPath(node)
                };
            }

            var tree = parseNode(document.documentElement, 0, 30);
            if (window.AndroidDevTools && window.AndroidDevTools.onDomTreeExtracted) {
                window.AndroidDevTools.onDomTreeExtracted(JSON.stringify(tree));
            }
        })();
    """.trimIndent()

    val EXTRACT_STORAGE_JS = """
        (function() {
            var localItems = [];
            try {
                for (var i = 0; i < localStorage.length; i++) {
                    var k = localStorage.key(i);
                    localItems.push({ key: k, value: localStorage.getItem(k) });
                }
            } catch(e) {}

            var sessionItems = [];
            try {
                for (var j = 0; j < sessionStorage.length; j++) {
                    var sk = sessionStorage.key(j);
                    sessionItems.push({ key: sk, value: sessionStorage.getItem(sk) });
                }
            } catch(e) {}

            if (window.AndroidDevTools && window.AndroidDevTools.onStorageExtracted) {
                window.AndroidDevTools.onStorageExtracted(
                    JSON.stringify(localItems),
                    JSON.stringify(sessionItems),
                    document.cookie || ""
                );
            }
        })();
    """.trimIndent()
}
