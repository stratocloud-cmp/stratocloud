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
public class ShellByShellExecutor implements RemoteScriptExecutor {

    private static final String DIRECTORY = "/etc/stratocloud/temp";

    @Override
    public ExecutionType getExecutionType() {
        return new ExecutionType(
                GuestCommandType.SHELL,
                RemoteScriptType.SHELL
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
            String filePath = createShellFile(executor, remoteScript, stdout, stderr);
            runShellFile(executor, filePath, remoteScript.programPath(), stdout, stderr);
            return RemoteScriptResult.succeed(stdout.toString(), stderr.toString());
        } catch (Exception e) {
            log.error("Failed to execute script: ", e);
            stdout.append(e);
            stderr.append(e);
            return RemoteScriptResult.failed(stdout.toString(), stderr.toString());
        }
    }

    private void createDirectory(GuestCommandExecutor executor, StringBuilder stdout, StringBuilder stderr) {
        String createDirectoryCmd = "sudo mkdir -p %s".formatted(DIRECTORY);
        executeCommand(executor, createDirectoryCmd, stdout, stderr);
    }

    private void runShellFile(GuestCommandExecutor executor,
                              String filePath,
                              String programPath,
                              StringBuilder stdout,
                              StringBuilder stderr) {
        if(Utils.isBlank(programPath))
            programPath = "/bin/bash";

        executeCommand(executor, "sudo %s %s".formatted(programPath, filePath), stdout, stderr);
    }


    private String createShellFile(GuestCommandExecutor executor,
                                   RemoteScript remoteScript,
                                   StringBuilder stdout,
                                   StringBuilder stderr) {
        String filePath = DIRECTORY + "/" + UUID.randomUUID()+".sh";
        String singleQuote = "'\"'\"'";
        String createFileCmd = "sudo echo '%s' > %s".formatted(
                remoteScript.content()
                        .replaceAll("\r\n", "\n")
                        .replaceAll("'", singleQuote),
                filePath
        );
        executeCommand(executor, createFileCmd, stdout, stderr);
        return filePath;
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
