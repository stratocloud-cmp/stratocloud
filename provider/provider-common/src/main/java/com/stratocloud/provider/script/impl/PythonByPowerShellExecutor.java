package com.stratocloud.provider.script.impl;

import com.stratocloud.exceptions.StratoException;
import com.stratocloud.provider.guest.GuestOsHandler;
import com.stratocloud.provider.guest.command.*;
import com.stratocloud.provider.script.RemoteScriptExecutor;
import com.stratocloud.provider.script.RemoteScriptResult;
import com.stratocloud.resource.Resource;
import com.stratocloud.script.RemoteScript;
import com.stratocloud.script.RemoteScriptType;
import com.stratocloud.utils.Utils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Slf4j
@Component
public class PythonByPowerShellExecutor implements RemoteScriptExecutor {

    private static final String DIRECTORY = "C:\\stratocloud\\temp";

    @Override
    public ExecutionType getExecutionType() {
        return new ExecutionType(
                GuestCommandType.POWERSHELL,
                RemoteScriptType.PYTHON
        );
    }

    @Override
    public RemoteScriptResult execute(GuestCommandExecutorFactory<?> commandExecutorFactory,
                                      Resource guestOsResource,
                                      RemoteScript remoteScript) {
        GuestOsHandler guestOsHandler = (GuestOsHandler) guestOsResource.getResourceHandler();

        StringBuilder stdout = new StringBuilder();
        StringBuilder stderr = new StringBuilder();

        try (var executor = commandExecutorFactory.createExecutor(guestOsHandler, guestOsResource)){
            createDirectory(executor, stdout, stderr);
            String filePath = createPythonFile(executor, stdout, stderr);
            setContent(executor, filePath, remoteScript, stdout, stderr);
            runPythonFile(executor, filePath, remoteScript.programPath(), stdout, stderr);
            return RemoteScriptResult.succeed(stdout.toString(), stderr.toString());
        } catch (Exception e) {
            log.error("Failed to execute script: ", e);
            stderr.append(e);
            stdout.append(e);
            return RemoteScriptResult.failed(stdout.toString(), stderr.toString());
        }
    }

    private void runPythonFile(GuestCommandExecutor executor,
                               String filePath,
                               String programPath,
                               StringBuilder stdout,
                               StringBuilder stderr) {
        if(Utils.isBlank(programPath))
            programPath = "python";

        executeCommand(executor, "%s %s".formatted(programPath, filePath), stdout, stderr);
    }

    private void setContent(GuestCommandExecutor executor,
                            String filePath,
                            RemoteScript remoteScript,
                            StringBuilder stdout,
                            StringBuilder stderr) {
        String cmd = "Set-Content -Path '%s' -Value '%s'".formatted(
                filePath, remoteScript.content()
        );
        executeCommand(executor, cmd, stdout, stderr);
    }

    private String createPythonFile(GuestCommandExecutor executor, StringBuilder stdout, StringBuilder stderr) {
        String filePath = DIRECTORY + "\\" + UUID.randomUUID()+".py";
        String createFileCmd = "New-Item -Path '%s' -ItemType File".formatted(filePath);
        executeCommand(executor, createFileCmd, stdout, stderr);
        return filePath;
    }

    private void createDirectory(GuestCommandExecutor executor, StringBuilder stdout, StringBuilder stderr){
        String createDirectoryCmd = """
                if(-not (Test-Path '%s')){
                  New-Item -Path '%s' -ItemType Directory
                }
                """.formatted(DIRECTORY,DIRECTORY);
        executeCommand(executor, createDirectoryCmd, stdout, stderr);
    }

    private static void executeCommand(GuestCommandExecutor executor,
                                                     String cmd,
                                                     StringBuilder stdout,
                                                     StringBuilder stderr) {
        GuestCommand guestCommand = new GuestCommand(cmd);
        GuestCommandResult result = executor.execute(guestCommand);

        if(Utils.isNotBlank(result.output()))
            stdout.append(result.output()).append("\n");

        if(result.status() != GuestCommandResult.Status.SUCCESS) {
            throw new StratoException(result.error());
        }

        if(Utils.isNotBlank(result.error()))
            stderr.append(result.error()).append("\n");
    }
}
