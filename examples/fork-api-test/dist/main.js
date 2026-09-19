"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.registerToolPkg = registerToolPkg;
exports.runForkApiTest = runForkApiTest;
const ID = "com.operit.fork.api_test";
function inspect(name, value, detail) { return { name, ok: Boolean(value), detail }; }
function runForkApiTest() {
    const results = [];
    results.push(inspect("toolpkg_runtime", typeof ToolPkg !== "undefined", "ToolPkg global is available"));
    results.push(inspect("lifecycle_registration", typeof ToolPkg?.registerAppLifecycleHook === "function", "App lifecycle bridge is available"));
    results.push(inspect("tool_lifecycle_registration", typeof ToolPkg?.registerToolLifecycleHook === "function", "Tool lifecycle bridge is available"));
    results.push(inspect("filesystem_runtime", typeof Tools?.Files?.write === "function", "Sandbox file API is available"));
    results.push(inspect("fork_api_runtime", typeof DeveloperApiRuntime !== "undefined" || typeof OperitDeveloperApi !== "undefined", "Fork API global, if exported by this host build"));
    return { packageId: ID, hostDebugPackage: "com.ai.assistance.operit.debug", results, note: "Reports visible capabilities without claiming unavailable globals." };
}
function registerToolPkg() {
    if (typeof ToolPkg?.registerAppLifecycleHook === "function") {
        ToolPkg.registerAppLifecycleHook({ event: "APP_STARTED", function: () => {
            try {
                const report = JSON.stringify(runForkApiTest(), null, 2);
                const path = `${ToolPkg.getConfigDir()}/fork_api_test_report.json`;
                Tools.Files.write(path, report);
                console.log(`[fork-api-test] report saved: ${path}`);
            } catch (error) { console.log(`[fork-api-test] failed: ${String(error)}`); }
            return null;
        } });
    }
    return true;
}
