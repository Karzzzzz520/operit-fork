package com.ai.assistance.operit.core.tools.javascript

/**
 * Builds the conservative-JS OperitFork namespace that is exposed to sandbox packages.
 *
 * Syntax rules for bootstrap scripts: no optional chaining, no arrow functions, no template
 * literals. The ToolPkg JS parser rejects modern syntax, so only var/function/ES5 constructs are
 * used here. The native side is reached through NativeInterface.operitForkInvoke, which routes to
 * ForkApiBridge and the public contracts under api/publicapi.
 */
internal fun buildForkApiBridgeScript(): String {
    return """
        (function() {
            var root = typeof globalThis !== 'undefined'
                ? globalThis
                : (typeof window !== 'undefined' ? window : this);

            var localSubscriptions = {};
            var localSubscriptionCounter = 0;

            function requireInvoke() {
                if (
                    typeof NativeInterface === 'undefined' ||
                    !NativeInterface ||
                    typeof NativeInterface.operitForkInvoke !== 'function'
                ) {
                    throw new Error('NativeInterface.operitForkInvoke is unavailable');
                }
                return NativeInterface.operitForkInvoke.bind(NativeInterface);
            }

            function invokeSync(method, params) {
                var payload = JSON.stringify({ method: method, params: params || {} });
                var raw = requireInvoke()(payload);
                var parsed = null;
                try {
                    parsed = JSON.parse(String(raw));
                } catch (parseError) {
                    parsed = null;
                }
                if (!parsed || typeof parsed !== 'object') {
                    return {
                        success: false,
                        errorCode: 'invalid_response',
                        message: String(raw)
                    };
                }
                return parsed;
            }

            function invoke(method, params) {
                var result = null;
                try {
                    result = invokeSync(method, params);
                } catch (error) {
                    result = {
                        success: false,
                        errorCode: 'bridge_error',
                        message: String(error && error.message ? error.message : error)
                    };
                }
                return Promise.resolve(result);
            }

            var secureTokenStore = {
                put: function(packageId, accountId, tokenJson) {
                    return invoke('secureTokenStore.put', {
                        packageId: packageId,
                        accountId: accountId,
                        tokenJson: tokenJson
                    });
                },
                get: function(packageId, accountId) {
                    return invoke('secureTokenStore.get', {
                        packageId: packageId,
                        accountId: accountId
                    });
                },
                delete: function(packageId, accountId) {
                    return invoke('secureTokenStore.delete', {
                        packageId: packageId,
                        accountId: accountId
                    });
                },
                listAccounts: function(packageId) {
                    return invoke('secureTokenStore.listAccounts', { packageId: packageId });
                },
                status: function(packageId, accountId) {
                    return invoke('secureTokenStore.status', {
                        packageId: packageId,
                        accountId: accountId
                    });
                }
            };

            var oauthCallback = {
                begin: function(request) {
                    return invoke('oauthCallback.begin', { request: request || {} });
                },
                consumeCallback: function(packageId, state, code, error) {
                    return invoke('oauthCallback.consumeCallback', {
                        packageId: packageId,
                        state: state,
                        code: code,
                        error: error
                    });
                },
                cancel: function(packageId, sessionId) {
                    return invoke('oauthCallback.cancel', {
                        packageId: packageId,
                        sessionId: sessionId
                    });
                }
            };

            var modelConfig = {
                list: function(packageId) {
                    return invoke('modelConfig.list', { packageId: packageId });
                },
                create: function(packageId, request) {
                    return invoke('modelConfig.create', {
                        packageId: packageId,
                        request: request || {}
                    });
                },
                update: function(packageId, configId, request) {
                    return invoke('modelConfig.update', {
                        packageId: packageId,
                        configId: configId,
                        request: request || {}
                    });
                },
                delete: function(packageId, configId) {
                    return invoke('modelConfig.delete', {
                        packageId: packageId,
                        configId: configId
                    });
                }
            };

            var aiProvider = {
                register: function(packageId, descriptor) {
                    return invoke('aiProvider.register', {
                        packageId: packageId,
                        descriptor: descriptor || {}
                    });
                },
                unregister: function(packageId, providerId) {
                    return invoke('aiProvider.unregister', {
                        packageId: packageId,
                        providerId: providerId
                    });
                },
                list: function(packageId) {
                    return invoke('aiProvider.list', { packageId: packageId });
                },
                execute: function(request) {
                    return invoke('aiProvider.execute', { request: request || {} });
                }
            };

            var controlledHttp = {
                execute: function(packageId, request) {
                    return invoke('controlledHttp.execute', {
                        packageId: packageId,
                        request: request || {}
                    });
                }
            };

            var eventBus = {
                publish: function(packageId, event) {
                    if (event && typeof event === 'object') {
                        var keys = Object.keys(localSubscriptions);
                        for (var index = 0; index < keys.length; index += 1) {
                            var subscription = localSubscriptions[keys[index]];
                            if (!subscription) {
                                continue;
                            }
                            if (subscription.type !== event.type && subscription.type !== '*') {
                                continue;
                            }
                            try {
                                subscription.handler(event);
                            } catch (handlerError) {
                            }
                        }
                    }
                    return invoke('eventBus.publish', {
                        packageId: packageId,
                        event: event || {}
                    });
                },
                subscribe: function(packageId, eventType, handler) {
                    if (typeof handler !== 'function') {
                        return Promise.resolve({
                            success: false,
                            errorCode: 'invalid_handler',
                            message: 'handler must be a function'
                        });
                    }
                    localSubscriptionCounter += 1;
                    var subscriptionId = 'fork_sub_' + localSubscriptionCounter;
                    localSubscriptions[subscriptionId] = {
                        packageId: packageId,
                        type: eventType,
                        handler: handler
                    };
                    return Promise.resolve({ success: true, value: subscriptionId });
                },
                unsubscribe: function(packageId, subscriptionId) {
                    delete localSubscriptions[subscriptionId];
                    return Promise.resolve({ success: true });
                }
            };

            var lifecycleHook = {
                onLoad: function(packageId) {
                    return invoke('lifecycleHook.onLoad', { packageId: packageId });
                },
                onEnable: function(packageId) {
                    return invoke('lifecycleHook.onEnable', { packageId: packageId });
                },
                onDisable: function(packageId) {
                    return invoke('lifecycleHook.onDisable', { packageId: packageId });
                },
                onUnload: function(packageId) {
                    return invoke('lifecycleHook.onUnload', { packageId: packageId });
                }
            };

            var developerDiagnostics = {
                record: function(packageId, event) {
                    return invoke('developerDiagnostics.record', {
                        packageId: packageId,
                        event: event || {}
                    });
                },
                query: function(packageId, limit) {
                    return invoke('developerDiagnostics.query', {
                        packageId: packageId,
                        limit: limit
                    });
                },
                clear: function(packageId) {
                    return invoke('developerDiagnostics.clear', { packageId: packageId });
                }
            };

            var tools = {
                invoke: function(toolName, params) {
                    return invoke('tools.invoke', { toolName: toolName, params: params || {} });
                }
            };

            var packages = {
                list: function() {
                    return invoke('packages.list', {});
                },
                enable: function(packageName) {
                    return invoke('packages.enable', { packageName: packageName });
                },
                disable: function(packageName) {
                    return invoke('packages.disable', { packageName: packageName });
                },
                isEnabled: function(packageName) {
                    return invoke('packages.isEnabled', { packageName: packageName });
                },
                reload: function() {
                    return invoke('packages.reload', {});
                }
            };

            var host = {
                getAppInfo: function() {
                    return invoke('host.appInfo', {});
                }
            };

            var update = {
                check: function(currentVersion) {
                    return invoke('update.check', { currentVersion: currentVersion });
                }
            };

            var OperitFork = {
                isAvailable: function() {
                    return true;
                },
                isCapabilityGranted: function(packageId, capability) {
                    var result = invokeSync('isCapabilityGranted', {
                        packageId: packageId,
                        capability: capability
                    });
                    return !!(result && result.success && result.value);
                },
                getHostInfo: function() {
                    return invoke('getHostInfo', {});
                },
                listCapabilities: function() {
                    return invoke('listCapabilities', {});
                },
                negotiate: function(manifest) {
                    return invoke('negotiate', manifest || {});
                },
                secureTokenStore: secureTokenStore,
                oauthCallback: oauthCallback,
                modelConfig: modelConfig,
                aiProvider: aiProvider,
                controlledHttp: controlledHttp,
                eventBus: eventBus,
                lifecycleHook: lifecycleHook,
                developerDiagnostics: developerDiagnostics,
                tools: tools,
                packages: packages,
                host: host,
                update: update
            };

            try {
                root.OperitFork = OperitFork;
            } catch (assignError) {
            }
        })();
    """.trimIndent()
}
