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
package net.fhirfactory.pegacorn.itops.im.workshops.transform.matrixbridge.common;

import net.fhirfactory.pegacorn.itops.im.valuesets.OAMRoomTypeEnum;

import javax.enterprise.context.ApplicationScoped;
import java.util.Locale;

@ApplicationScoped
public class ParticipantRoomIdentityFactory {

    protected String flattenParticipantName(String participantName){
        String participantNameToLowerCase = participantName.toLowerCase(Locale.ROOT);
        String participantNameWithDotsConvertedToHyphens = participantNameToLowerCase.replace(".", "-");
        String participantNameWithUnderscoresConvertedToHyphens = participantNameWithDotsConvertedToHyphens.replace("_", "-");
        String participantNameWithSlashesConvertedToHyphens = participantNameWithUnderscoresConvertedToHyphens.replace("/", "-");
        String participantNameWithColonsConvertedToHyphens = participantNameWithSlashesConvertedToHyphens.replace(":", "-");
        return(participantNameWithColonsConvertedToHyphens);
    }

    protected String buildRoomPseudoAlias(String participantName, OAMRoomTypeEnum oamRoomType){
        String flattenedParticipantName = flattenParticipantName(participantName);
        String aliasId =oamRoomType.getAliasPrefix().toLowerCase(Locale.ROOT) + flattenedParticipantName;
        return(aliasId);
    }

    public String buildWorkshopSpacePseudoAlias(String participantName){
        String aliasId = buildRoomPseudoAlias(participantName, OAMRoomTypeEnum.OAM_ROOM_TYPE_WORKSHOP);
        return(aliasId);
    }

    public String buildWorkUnitProcessorSpacePseudoAlias( String participantName){
        String aliasId = buildRoomPseudoAlias(participantName, OAMRoomTypeEnum.OAM_ROOM_TYPE_WUP);
        return(aliasId);
    }

    public String buildProcessingPlantSpacePseudoAlias( String participantName){
        String aliasId = buildRoomPseudoAlias(participantName, OAMRoomTypeEnum.OAM_ROOM_TYPE_SUBSYSTEM);
        return(aliasId);
    }

    public String buildEndpointSpacePseudoAlias( String participantName){
        String aliasId = buildRoomPseudoAlias(participantName, OAMRoomTypeEnum.OAM_ROOM_TYPE_ENDPOINT);
        return(aliasId);
    }

    public String buildWUPRoomPseudoAlias(String wupParticipantName, OAMRoomTypeEnum oamRoomType){
        String aliasId = buildRoomPseudoAlias(wupParticipantName, oamRoomType);
        return(aliasId);
    }

    public String buildProcessingPlantRoomPseudoAlias(String processingPlantParticipantName, OAMRoomTypeEnum roomType) {
        String aliasId = buildRoomPseudoAlias(processingPlantParticipantName, roomType);
        return(aliasId);
    }

    public String buildEndpointRoomPseudoAlias(String endpointParticipantName, OAMRoomTypeEnum oamRoomTypeEnum){
        String aliasId = buildRoomPseudoAlias(endpointParticipantName, oamRoomTypeEnum);
        return(aliasId);

    }

    public String buildOAMRoomPseudoAlias(String participantName, OAMRoomTypeEnum oamRoomTypeEnum){
        String aliasId = buildRoomPseudoAlias(participantName, oamRoomTypeEnum);
        return(aliasId);
    }

}
