/*
 * Copyright (c) 2021 Mark A. Hunter
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package net.fhirfactory.pegacorn.itops.im.valuesets;

import org.apache.commons.lang3.StringUtils;

public enum OAMRoomTypeEnum {
    OAM_ROOM_TYPE_SUBSYSTEM("Subsystem", "subsystem-"),
    OAM_ROOM_TYPE_SUBSYSTEM_TASKS("ActivityReports", "subsystem-tasks-"),
    OAM_ROOM_TYPE_SUBSYSTEM_METRICS("Metrics", "subsystem-metrics-"),
    OAM_ROOM_TYPE_SUBSYSTEM_EVENTS("Log", "subsystem-log-"),
    OAM_ROOM_TYPE_SUBSYSTEM_SUBSCRIPTIONS("Subscriptions", "subsystem-subscriptions-"),
    OAM_ROOM_TYPE_SUBSYSTEM_COMPONENTS("Components", "subsystem-components-"),
    OAM_ROOM_TYPE_WORKSHOP("Workshop", "workshop-"),
    OAM_ROOM_TYPE_WUP("WorkUnitProcessor", "wup-"),
    OAM_ROOM_TYPE_WUP_TASKS("ActivityReports", "wup-tasks-"),
    OAM_ROOM_TYPE_WUP_METRICS("Metrics", "wup-metrics-"),
    OAM_ROOM_TYPE_WUP_EVENTS("Log", "wup-log-"),
    OAM_ROOM_TYPE_WUP_SUBSCRIPTIONS("Subscriptions", "wup-subscriptions-"),
    OAM_ROOM_TYPE_ENDPOINT("Endpoint", "endpoint-"),
    OAM_ROOM_TYPE_ENDPOINT_METRICS("Metrics", "endpoint-metrics-"),
    OAM_ROOM_TYPE_ENDPOINT_EVENTS("Log", "endpoint-log-");

    private String displayName;
    private String aliasPrefix;

    private OAMRoomTypeEnum(String displayName, String aliasPrefix){
        this.displayName = displayName;
        this.aliasPrefix = aliasPrefix;
    }

    public String getAliasPrefix() {
        return (this.aliasPrefix);
    }

    public String getDisplayName(){
        return(this.displayName);
    }

    public static final OAMRoomTypeEnum fromDisplayName(String displayName){
        if(StringUtils.isEmpty(displayName)){
            return(null);
        }
        for(OAMRoomTypeEnum currentRoomType: OAMRoomTypeEnum.values()){
            if(currentRoomType.getDisplayName().equalsIgnoreCase(displayName)){
                return(currentRoomType);
            }
        }
        return(null);
    }

    public static final OAMRoomTypeEnum fromAliasPrefix(String aliasPrefix){
        if(StringUtils.isEmpty(aliasPrefix)){
            return(null);
        }
        for(OAMRoomTypeEnum currentRoomType: OAMRoomTypeEnum.values()){
            if(aliasPrefix.startsWith(currentRoomType.getAliasPrefix())){
                return(currentRoomType);
            }
        }
        return(null);
    }
}
