package com.stratocloud.provider.guest.command;

public record GuestCommandResult(Status status, String output, String error) {

    public enum Status{
        SUCCESS,
        FAILED
    }


    public static GuestCommandResult failed(String output, String error){
        return new GuestCommandResult(Status.FAILED, output, error);
    }

    public static GuestCommandResult succeed(String output, String error) {
        return new GuestCommandResult(Status.SUCCESS, output, error);
    }
}
