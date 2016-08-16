/*
 * $Id$
 *
 * Copyright (C) 2010 by Brockmann Consult (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the
 * Free Software Foundation. This program is distributed in the hope it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package org.esa.cci.sst.assessment;

import py4j.GatewayServer;

public class CarWebToolApp {

    private final CarToolFasade fassade;

    public CarWebToolApp() {
        fassade = new CarToolFasade();
    }

    public CarToolFasade getFassade() {
        return fassade;
    }

    public static void main(String[] args) {
        final GatewayServer server = new GatewayServer(new CarWebToolApp());
        server.start();
        System.out.println("CarTool Gateway Server Started");
    }
}
