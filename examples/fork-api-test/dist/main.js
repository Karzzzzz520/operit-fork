"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.registerToolPkg = registerToolPkg;
exports.runForkApiTest = runForkApiTest;

var PACKAGE_ID = "com.operit.fork.api_test";

function hasFunction(object, name) {
    return object != null && typeof object[name] === "function";
}

function check(name, ok, detail) {
    return { name: name, ok: !!ok, detail: detail };
}

function runForkApiTest() {
    var results = [];
    var toolPkgAvailable = typeof ToolPkg !== "undefined";
    var toolsAvailable = typeof Tools !== "undefined";

    results.push(check("toolpkg_runtime", toolPkgAvailable, "ToolPkg global is available"));
    results.push(check("app_lifecycle_registration", hasFunction(toolPkgAvailable ? ToolPkg : null, "registerAppLifecycleHook"), "App lifecycle registration API"));
    results.push(check("tool_lifecycle_registration", hasFunction(toolPkgAvailable ? ToolPkg : null, "registerToolLifecycleHook"), "Tool lifecycle registration API"));
    results.push(check("filesystem_runtime", toolsAvailable && Tools.Files != null && hasFunction(Tools.Files, "write"), "Sandbox file API"));
    results.push(check("fork_host_bridge", typeof DeveloperApiRuntime !== "undefined" || typeof OperitDeveloperApi !== "undefined", "Fork host bridge global, when exported by the host"));

    return {
        packageId: PACKAGE_ID,
        hostDebugPackage: "com.ai.assistance.operit.debug",
        results: results,
        note: "This report only records capabilities visible to the ToolPkg runtime."
    };
}

function writeReport() {
    try {
        var report = JSON.stringify(runForkApiTest(), null, 2);
        var path = ToolPkg.getConfigDir() + "/fork_api_test_report.json";
        Tools.Files.write(path, report);
        console.log("[fork-api-test] report saved: " + path);
    } catch (error) {
        console.log("[fork-api-test] report failed: " + String(error));
    }
    return null;
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
