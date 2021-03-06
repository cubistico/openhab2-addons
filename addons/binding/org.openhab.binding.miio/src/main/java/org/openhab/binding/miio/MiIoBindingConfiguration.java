/**
 * Copyright (c) 2010-2019 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.miio;

/**
 * The {@link MiIoBindingConfiguration} class defines variables which are
 * used for the binding configuration.
 *
 * @author Marcel Verpaalen - Initial contribution
 */
public final class MiIoBindingConfiguration {
    public String host;
    public String token;
    public String deviceId;
    public String model;
    public int refreshInterval;
    public int timeout;
}
