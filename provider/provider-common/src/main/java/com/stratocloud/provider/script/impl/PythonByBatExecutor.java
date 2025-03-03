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
public class PythonByBatExecutor implements RemoteScriptExecutor {

    private static final String DIRECTORY = "C:\\stratocloud\\temp";

    @Override
    public ExecutionType getExecutionType() {
        return new ExecutionType(
                GuestCommandType.BAT,
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
            String filePath = createPythonFile(executor, remoteScript, stdout, stderr);
            runPythonFile(executor, filePath, stdout, stderr);
            return RemoteScriptResult.succeed(stdout.toString(), stderr.toString());
        } catch (Exception e) {
            log.error("Failed to execute script: ", e);
            stderr.append(e);
            return RemoteScriptResult.failed(stdout.toString(), stderr.toString());
        }
    }

    private void runPythonFile(GuestCommandExecutor executor,
                               String filePath,
                               StringBuilder stdout,
                               StringBuilder stderr) {
        String cmd = "python %s".formatted(filePath);
        executeCommand(executor, cmd, stdout, stderr);
    }

    private String createPythonFile(GuestCommandExecutor executor,
                                    RemoteScript remoteScript,
                                    StringBuilder stdout,
                                    StringBuilder stderr) {
        String filePath = DIRECTORY + "\\" + UUID.randomUUID()+".py";
        String createFileCmd = "echo %s > %s".formatted(remoteScript.content(), filePath);
        executeCommand(executor, createFileCmd, stdout, stderr);
        return filePath;
    }

    private void createDirectory(GuestCommandExecutor executor, StringBuilder stdout, StringBuilder stderr){
        String createDirectoryCmd = """
                if not exist "%s" (
                  mkdir "%s"
                )
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
            stdout.append(result.output());

        if(result.status() != GuestCommandResult.Status.SUCCESS) {
            throw new StratoException(result.error());
        }

        if(Utils.isNotBlank(result.error()))
            stderr.append(result.error());
    }
}
