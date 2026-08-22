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

            var origFetch = window.fetch;
            if (origFetch) {
                window.fetch = function() {
                    var args = arguments;
                    var url = typeof args[0] === 'string' ? args[0] : (args[0] && args[0].url ? args[0].url : 'Fetch');
                    var method = (args[1] && args[1].method) ? args[1].method : 'GET';
                    var reqBody = (args[1] && args[1].body) ? String(args[1].body) : '';
                    var startTime = Date.now();

                    return origFetch.apply(this, args).then(function(response) {
                        var duration = Date.now() - startTime;
                        var status = response.status;
                        var clone = response.clone();
                        var resHeaders = {};
                        try {
                            clone.headers.forEach(function(val, key) { resHeaders[key] = val; });
                        } catch(e) {}

                        clone.text().then(function(bodyText) {
                            if (window.AndroidDevTools && window.AndroidDevTools.onNetworkRequest) {
                                window.AndroidDevTools.onNetworkRequest(
                                    url, method, status, 'fetch', duration,
                                    '{}', reqBody, JSON.stringify(resHeaders), bodyText.substring(0, 4000)
                                );
                            }
                        }).catch(function() {
                            if (window.AndroidDevTools && window.AndroidDevTools.onNetworkRequest) {
                                window.AndroidDevTools.onNetworkRequest(
                                    url, method, status, 'fetch', duration,
                                    '{}', reqBody, JSON.stringify(resHeaders), '[Binary or Stream response]'
                                );
                            }
                        });

                        return response;
                    }).catch(function(err) {
                        var duration = Date.now() - startTime;
                        if (window.AndroidDevTools && window.AndroidDevTools.onNetworkRequest) {
                            window.AndroidDevTools.onNetworkRequest(
                                url, method, 0, 'fetch', duration,
                                '{}', reqBody, '{}', 'Network Request Failed: ' + (err.message || 'Error')
                            );
                        }
                        throw err;
                    });
                };
            }

            var origOpen = XMLHttpRequest.prototype.open;
            var origSend = XMLHttpRequest.prototype.send;
            XMLHttpRequest.prototype.open = function(method, url) {
                this._url = url;
                this._method = method;
                this._startTime = Date.now();
                return origOpen.apply(this, arguments);
            };
            XMLHttpRequest.prototype.send = function(body) {
                var xhr = this;
                this.addEventListener('load', function() {
                    var duration = Date.now() - (xhr._startTime || Date.now());
                    if (window.AndroidDevTools && window.AndroidDevTools.onNetworkRequest) {
                        window.AndroidDevTools.onNetworkRequest(
                            xhr._url || 'XHR', xhr._method || 'GET', xhr.status, 'xhr', duration,
                            '{}', body ? String(body) : '', xhr.getAllResponseHeaders() || '{}',
                            (xhr.responseText || '').substring(0, 4000)
                        );
                    }
                });
                this.addEventListener('error', function() {
                    if (window.AndroidDevTools && window.AndroidDevTools.onNetworkRequest) {
                        window.AndroidDevTools.onNetworkRequest(
                            xhr._url || 'XHR', xhr._method || 'GET', 0, 'xhr', 0,
                            '{}', body ? String(body) : '', '{}', 'XHR Error'
                        );
                    }
                });
                return origSend.apply(this, arguments);
            };
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
                        outerHtml: el.outerHTML ? el.outerHTML.substring(0, 2000) : "",
                        innerHtml: el.innerHTML ? el.innerHTML.substring(0, 1000) : "",
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
            function parseNode(node, depth, maxDepth) {
                if (!node || depth > maxDepth) return null;

                if (node.nodeType === 3) { // Text node
                    var text = (node.textContent || '').trim();
                    if (!text) return null;
                    return {
                        nodeId: "text_" + Math.random().toString(36).substr(2, 6),
                        tagName: "#text",
                        isTextNode: true,
                        textContent: text.length > 80 ? text.substring(0, 80) + '...' : text,
                        attributes: {},
                        children: []
                    };
                }

                if (node.nodeType !== 1) return null; // Only Element nodes

                var tag = node.tagName.toLowerCase();
                if (tag === 'script' || tag === 'style' || tag === 'svg' || tag === 'path') {
                    // Summarize heavy tags
                    return {
                        nodeId: "el_" + tag + "_" + Math.random().toString(36).substr(2, 6),
                        tagName: tag,
                        isTextNode: false,
                        textContent: "",
                        attributes: { id: node.id || "", class: node.className || "" },
                        children: []
                    };
                }

                var attrs = {};
                for (var i = 0; i < node.attributes.length; i++) {
                    var a = node.attributes[i];
                    attrs[a.name] = a.value;
                }

                var childrenList = [];
                var child = node.firstChild;
                var childCount = 0;
                while (child && childCount < 30) {
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
                    children: childrenList
                };
            }

            var tree = parseNode(document.documentElement, 0, 5);
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
