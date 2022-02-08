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
package net.fhirfactory.pegacorn.itops.im.workshops.datagrid.topologymaps;

import net.fhirfactory.pegacorn.communicate.matrix.model.core.MatrixUser;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@ApplicationScoped
public class ITOpsKnownUserMapDM {
    private static final Logger LOG = LoggerFactory.getLogger(ITOpsKnownUserMapDM.class);

    private ConcurrentHashMap<String, MatrixUser> knownUserSet;
    private Object knownUserSetLock;

    private ConcurrentHashMap<String, MatrixUser> previousKnownUserSet;
    private Object previousKnownUserSetLock;

    //
    // Constructor(s)
    //

    public ITOpsKnownUserMapDM(){
        this.previousKnownUserSet = new ConcurrentHashMap<>();
        this.knownUserSet = new ConcurrentHashMap<>();
        this.knownUserSetLock = new Object();
        this.previousKnownUserSetLock = new Object();
    }

    //
    // Getters (and Setters)
    //

    public ConcurrentHashMap<String, MatrixUser> getKnownUserSet() {
        return knownUserSet;
    }

    public ConcurrentHashMap<String, MatrixUser> getPreviousKnownUserSet() {
        return previousKnownUserSet;
    }

    protected Logger getLogger(){
        return(LOG);
    }

    //
    // Business Methods
    //

    public void addMatrixUser(MatrixUser user){
        if(user == null){
            return;
        }
        if(StringUtils.isEmpty(user.getName())){
            return;
        }
        synchronized (knownUserSetLock){
            if(knownUserSet.containsKey(user.getName())){
                knownUserSet.remove(user.getName());
            }
            knownUserSet.put(user.getName(), user);
        }
    }

    public void removeMatrixUser(String userName){
        if(StringUtils.isEmpty(userName)){
            return;
        }
        synchronized (knownUserSetLock){
            if(knownUserSet.containsKey(userName)){
                knownUserSet.remove(userName);
            }
        }
    }

    public Set<MatrixUser> getKnownUsers(){
        Set<MatrixUser> knownUsers = new HashSet<>();
        if(knownUserSet.isEmpty()){
            return(knownUsers);
        }
        synchronized (knownUserSetLock){
            knownUsers.addAll(knownUserSet.values());
        }
        return(knownUsers);
    }

    public Set<MatrixUser> getRecentAddedUsers(){
        Set<MatrixUser> addedUsers = new HashSet<>();
        if(knownUserSet.isEmpty()){
            return(addedUsers);
        }
        if(previousKnownUserSet.isEmpty()){
            synchronized (knownUserSetLock){
                addedUsers.addAll(knownUserSet.values());
            }
        } else{
            synchronized (knownUserSetLock){
                Collection<MatrixUser> knownUsers = knownUserSet.values();
                for(MatrixUser currentKnownUser: knownUsers){
                    if(!previousKnownUserSet.containsKey(currentKnownUser.getName())){
                        addedUsers.add(currentKnownUser);
                    }
                }
            }
        }
        previousKnownUserSet.clear();
        Collection<MatrixUser> knownUsers = knownUserSet.values();
        for(MatrixUser currentKnownUser: knownUsers){
            previousKnownUserSet.put(currentKnownUser.getName(), currentKnownUser);
        }
        return(addedUsers);
    }
}
