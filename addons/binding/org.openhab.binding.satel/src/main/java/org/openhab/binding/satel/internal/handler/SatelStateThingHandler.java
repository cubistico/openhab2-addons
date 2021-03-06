/**
 * Copyright (c) 2010-2019 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.satel.internal.handler;

import java.util.Collection;
import java.util.LinkedList;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.lang.StringUtils;
import org.eclipse.smarthome.core.thing.Channel;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.types.Command;
import org.eclipse.smarthome.core.types.RefreshType;
import org.openhab.binding.satel.internal.command.IntegraStateCommand;
import org.openhab.binding.satel.internal.command.SatelCommand;
import org.openhab.binding.satel.internal.event.ConnectionStatusEvent;
import org.openhab.binding.satel.internal.event.IntegraStateEvent;
import org.openhab.binding.satel.internal.event.NewStatesEvent;
import org.openhab.binding.satel.internal.event.SatelEvent;
import org.openhab.binding.satel.internal.types.StateType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link SatelStateThingHandler} is base thing handler class for all state holding things.
 *
 * @author Krzysztof Goworek - Initial contribution
 */
public abstract class SatelStateThingHandler extends SatelThingHandler {

    private final Logger logger = LoggerFactory.getLogger(SatelStateThingHandler.class);

    private AtomicBoolean requiresRefresh;

    public SatelStateThingHandler(Thing thing) {
        super(thing);
        this.requiresRefresh = new AtomicBoolean(true);
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        logger.debug("New command for {}: {}", channelUID, command.toFullString());

        if (command == RefreshType.REFRESH) {
            this.requiresRefresh.set(true);
        } else if (bridgeHandler != null && StringUtils.isNotEmpty(bridgeHandler.getUserCode())) {
            SatelCommand satelCommand = convertCommand(channelUID, command);
            if (satelCommand != null) {
                bridgeHandler.sendCommand(satelCommand, true);
            }
        }
    }

    @Override
    public void incomingEvent(SatelEvent event) {
        logger.trace("Handling incoming event: {}", event);
        if (event instanceof ConnectionStatusEvent) {
            ConnectionStatusEvent statusEvent = (ConnectionStatusEvent) event;
            // we have just connected, change thing's status and force refreshing
            if (statusEvent.isConnected()) {
                updateStatus(ThingStatus.ONLINE);
                requiresRefresh.set(true);
            }
        } else if (event instanceof NewStatesEvent) {
            // refresh all states that have changed
            for (SatelCommand command : getRefreshCommands((NewStatesEvent) event)) {
                bridgeHandler.sendCommand(command, true);
            }
        } else if (event instanceof IntegraStateEvent) {
            // update thing's state unless it should accept commands only
            IntegraStateEvent stateEvent = (IntegraStateEvent) event;
            if (thingConfig.isCommandOnly()) {
                return;
            }
            for (Channel channel : getThing().getChannels()) {
                ChannelUID channelUID = channel.getUID();
                StateType stateType = getStateType(channelUID.getId());
                if (stateType != null && stateEvent.hasDataForState(stateType)) {
                    int bitNbr = thingConfig.getId() - 1;
                    boolean invertState = thingConfig.isStateInverted();
                    updateSwitch(channelUID, stateEvent.isSet(stateType, bitNbr) ^ invertState);
                }
            }
        }
    }

    protected abstract SatelCommand convertCommand(ChannelUID channel, Command command);

    protected abstract StateType getStateType(String channelId);

    protected Channel getChannel(StateType stateType) {
        String channelId = stateType.toString().toLowerCase();
        Channel channel = getThing().getChannel(channelId);
        if (channel == null) {
            logger.debug("Missing channel for {}", stateType);
        }
        return channel;
    }

    protected Collection<SatelCommand> getRefreshCommands(NewStatesEvent event) {
        Collection<SatelCommand> result = new LinkedList<>();
        boolean forceRefresh = requiresRefresh();
        for (Channel channel : getThing().getChannels()) {
            if (isLinked(channel.getUID().getId())) {
                StateType stateType = getStateType(channel.getUID().getId());
                if (forceRefresh || event.isNew(stateType.getRefreshCommand())) {
                    result.add(new IntegraStateCommand(stateType, bridgeHandler.getIntegraType().hasExtPayload()));
                }
            }
        }
        return result;
    }

    protected boolean requiresRefresh() {
        return requiresRefresh.getAndSet(false);
    }

}
