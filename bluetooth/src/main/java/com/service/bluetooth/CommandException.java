/*
 * Copyright (C) 2007 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.service.bluetooth;

/**
 * {@hide}
 */
public class CommandException extends RuntimeException
{
    Error e;

    public enum Error
    {
        TIME_OUT,
        INVALID_RESPONSE,
        BT_NOT_AVAILABLE,
        GENERIC_FAILURE,
        INVALID_ARGUMENT,
        ERR_SERVICE_ALREADY_STARTING,
        ERR_NO_ONGOING_CALL,
    }

    public CommandException(Error e)
    {
        super(e.toString());
        this.e = e;
    }

    public static CommandException fromBTErrno(int BT_errno)
    {
        switch (BT_errno)
        {
            case BTConstants.SUCCESS:
                return null;
            case BTConstants.BT_ERRNO_INVALID_RESPONSE:
                return new CommandException(Error.INVALID_RESPONSE);
            case BTConstants.BT_NOT_AVAILABLE:
                return new CommandException(Error.BT_NOT_AVAILABLE);
            case BTConstants.GENERIC_FAILURE:
                return new CommandException(Error.GENERIC_FAILURE);
            case BTConstants.INVALID_ARGUMENT:
                return new CommandException(Error.INVALID_ARGUMENT);
            case BTConstants.TIME_OUT:
                return new CommandException(Error.TIME_OUT);
            case BTConstants.ERR_SERVICE_ALREADY_STARTING:
                return new CommandException(Error.ERR_SERVICE_ALREADY_STARTING);
            case BTConstants.ERR_NO_ONGOING_CALL:
                return new CommandException(Error.ERR_NO_ONGOING_CALL);
            default:
                WLog.e("GSM", "Unrecognized BT errno " + BT_errno);
                return new CommandException(Error.INVALID_RESPONSE);
        }
    }

    public Error getCommandError()
    {
        return e;
    }


}
