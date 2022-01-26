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
package net.fhirfactory.pegacorn.itops.im.datatypes;

import java.io.Serializable;

public class ProcessingPlantSpaceDetail implements Serializable {
        private String participantName;
        private String participantAlias;
        private String participantSpaceId;
        private String componentSpaceName;
        private String componentSpaceAlias;
        private String componentSpaceId;

        //
        // Constructor(s)
        //

        public ProcessingPlantSpaceDetail(){
                this.participantSpaceId = null;
                this.participantName = null;
                this.participantAlias = null;
                this.componentSpaceAlias = null;
                this.componentSpaceName = null;
                this.componentSpaceId = null;
        }

        //
        // Getters and Setters
        //

        public String getParticipantName() {
                return participantName;
        }

        public void setParticipantName(String participantName) {
                this.participantName = participantName;
        }

        public String getParticipantAlias() {
                return participantAlias;
        }

        public void setParticipantAlias(String participantAlias) {
                this.participantAlias = participantAlias;
        }

        public String getParticipantSpaceId() {
                return participantSpaceId;
        }

        public void setParticipantSpaceId(String participantSpaceId) {
                this.participantSpaceId = participantSpaceId;
        }

        public String getComponentSpaceName() {
                return componentSpaceName;
        }

        public void setComponentSpaceName(String componentSpaceName) {
                this.componentSpaceName = componentSpaceName;
        }

        public String getComponentSpaceAlias() {
                return componentSpaceAlias;
        }

        public void setComponentSpaceAlias(String componentSpaceAlias) {
                this.componentSpaceAlias = componentSpaceAlias;
        }

        public String getComponentSpaceId() {
                return componentSpaceId;
        }

        public void setComponentSpaceId(String componentSpaceId) {
                this.componentSpaceId = componentSpaceId;
        }

        //
        // To String
        //

        @Override
        public String toString() {
                return "ProcessingPlantSpaceDetail{" +
                        "participantName='" + participantName + '\'' +
                        ", participantAlias='" + participantAlias + '\'' +
                        ", participantSpaceId='" + participantSpaceId + '\'' +
                        ", componentSpaceName='" + componentSpaceName + '\'' +
                        ", componentSpaceAlias='" + componentSpaceAlias + '\'' +
                        ", componentSpaceId='" + componentSpaceId + '\'' +
                        '}';
        }
}
