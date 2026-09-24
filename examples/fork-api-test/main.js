"use strict";

var PACKAGE_ID = "com.operit.fork.api_test";
var TEST_ACCOUNT_ID = "fork-api-self-test";

function hasFunction(object, name) {
    return object != null && typeof object[name] === "function";
}

function check(name, ok, detail) {
    return { name: name, ok: !!ok, detail: detail == null ? "" : String(detail) };
}

function collectOperitForkChecks() {
    var results = [];
    var forkAvailable = typeof OperitFork !== "undefined" && OperitFork != null;
    results.push(check("operitfork_global", forkAvailable, "OperitFork global is available"));
    if (!forkAvailable) {
        return Promise.resolve(results);
    }
    results.push(check("is_available", hasFunction(OperitFork, "isAvailable") && OperitFork.isAvailable() === true, "OperitFork.isAvailable()"));
    results.push(check("list_capabilities", hasFunction(OperitFork, "listCapabilities"), "OperitFork.listCapabilities()"));
    results.push(check("get_host_info", hasFunction(OperitFork, "getHostInfo"), "OperitFork.getHostInfo()"));
    results.push(check("secure_token_store", hasFunction(OperitFork, "secureTokenStore"), "OperitFork.secureTokenStore"));

    var tokenJson = '{"access_token":"fork-api-self-test"}';
    return OperitFork.secureTokenStore
        .put(PACKAGE_ID, TEST_ACCOUNT_ID, tokenJson)
        .then(function (putResult) {
            results.push(check("token_put", putResult && putResult.success, putResult && putResult.errorCode));
            return OperitFork.secureTokenStore.get(PACKAGE_ID, TEST_ACCOUNT_ID);
        })
        .then(function (getResult) {
            results.push(check("token_get", getResult && getResult.success, getResult && getResult.value));
            return OperitFork.listCapabilities();
        })
        .then(function (capabilitiesResult) {
            var count = capabilitiesResult && capabilitiesResult.success && capabilitiesResult.value ? capabilitiesResult.value.length : 0;
            results.push(check("capabilities_count", count >= 8, "capabilities=" + count));
            return OperitFork.getHostInfo();
        })
        .then(function (hostInfoResult) {
            var info = hostInfoResult && hostInfoResult.success ? hostInfoResult.value : null;
            results.push(check("host_injected", info != null && info.injected === true, info != null ? info.host : ""));
            return results;
        })
        .catch(function (error) {
            results.push(check("probe_error", false, String(error && error.message ? error.message : error)));
            return results;
        });
}

function runForkApiTest() {
    var toolPkgAvailable = typeof ToolPkg !== "undefined";
    var toolsAvailable = typeof Tools !== "undefined";
    var baseResults = [];
    baseResults.push(check("toolpkg_runtime", toolPkgAvailable, "ToolPkg global is available"));
    baseResults.push(check("app_lifecycle_registration", hasFunction(toolPkgAvailable ? ToolPkg : null, "registerAppLifecycleHook"), "App lifecycle registration API"));
    baseResults.push(check("filesystem_runtime", toolsAvailable && Tools.Files != null && hasFunction(Tools.Files, "write"), "Sandbox file API"));
    return collectOperitForkChecks().then(function (forkResults) {
        var results = baseResults.concat(forkResults);
        var passed = 0;
        for (var index = 0; index < results.length; index += 1) {
            if (results[index].ok) {
                passed += 1;
            }
        }
        return {
            packageId: PACKAGE_ID,
            passed: passed,
            total: results.length,
            results: results,
            note: "Checks the injected OperitFork namespace and the ToolPkg runtime."
        };
    });
}

function writeReport() {
    return runForkApiTest().then(function (report) {
        try {
            var text = JSON.stringify(report, null, 2);
            var path = ToolPkg.getConfigDir(PACKAGE_ID) + "/fork_api_test_report.json";
            Tools.Files.write(path, text);
            console.log("[fork-api-test] report saved: " + path);
            console.log("[fork-api-test] passed " + report.passed + "/" + report.total);
        } catch (error) {
            console.log("[fork-api-test] report failed: " + String(error));
        }
        return report;
    });
}

function registerToolPkg() {
    if (typeof ToolPkg === "undefined") {
        return false;
    }
    if (hasFunction(ToolPkg, "registerAppLifecycleHook")) {
        ToolPkg.registerAppLifecycleHook({
            id: "fork_api_test_startup",
            event: "application_on_create",
            function: writeReport
        });
    }
    return true;
}

exports.registerToolPkg = registerToolPkg;
exports.runForkApiTest = runForkApiTest;
exports.writeReport = writeReport;
