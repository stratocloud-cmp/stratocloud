package com.stratocloud.provider.guest.command;

import com.stratocloud.resource.OsType;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class StratoGuestCommandExecutorFactoryRegistry {
    private static final List<StratoGuestCommandExecutorFactory> factories = new ArrayList<>();

    public synchronized static void register(StratoGuestCommandExecutorFactory factory){
        factories.add(factory);
    }

    public static Optional<StratoGuestCommandExecutorFactory> getByCommandType(GuestCommandType commandType){
        return factories.stream().filter(f -> f.getCommandType() == commandType).findAny();
    }

    public static Optional<StratoGuestCommandExecutorFactory> getByOsType(OsType osType){
        return switch (osType) {
            case Linux -> getByCommandType(GuestCommandType.SHELL);
            case Windows -> getByCommandType(GuestCommandType.POWERSHELL);
            default -> Optional.empty();
        };
    }
}
