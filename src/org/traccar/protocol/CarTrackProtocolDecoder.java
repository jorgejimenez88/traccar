/*
 * Copyright 2014 - 2015 Anton Tananaev (anton.tananaev@gmail.com)
 * Copyright 2014 Rohit
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.traccar.protocol;

import org.jboss.netty.channel.Channel;
import org.traccar.BaseProtocolDecoder;
import org.traccar.helper.DateBuilder;
import org.traccar.helper.Parser;
import org.traccar.helper.PatternBuilder;
import org.traccar.model.Event;
import org.traccar.model.Position;

import java.net.SocketAddress;
import java.util.regex.Pattern;

public class CarTrackProtocolDecoder extends BaseProtocolDecoder {

    public CarTrackProtocolDecoder(CarTrackProtocol protocol) {
        super(protocol);
    }

    private static final Pattern PATTERN = new PatternBuilder()
            .text("$$")                          // header
            .number("(d+)")                      // device id
            .text("?").expression("*")
            .text("&A")
            .number("(dddd)")                    // command
            .text("&B")
            .number("(dd)(dd)(dd).(ddd),")       // time
            .expression("([AV]),")               // validity
            .number("(dd)(dd.dddd),")            // latitude
            .expression("([NS]),")
            .number("(ddd)(dd.dddd),")           // longitude
            .expression("([EW]),")
            .number("(d+.d*)?,")                 // speed
            .number("(d+.d*)?,")                 // course
            .number("(dd)(dd)(dd)")              // date (ddmmyy)
            .any()
            .expression("&C([^&]*)")             // io
            .expression("&D([^&]*)")             // odometer
            .expression("&E([^&]*)")             // alarm
            .expression("&Y([^&]*)").optional()  // adc
            .compile();

    @Override
    protected Object decode(
            Channel channel, SocketAddress remoteAddress, Object msg) throws Exception {

        Parser parser = new Parser(PATTERN, (String) msg);
        if (!parser.matches()) {
            return null;
        }

        Position position = new Position();
        position.setProtocol(getProtocolName());

        if (!identify(parser.next(), channel, remoteAddress)) {
            return null;
        }
        position.setDeviceId(getDeviceId());

        position.set("command", parser.next());

        DateBuilder dateBuilder = new DateBuilder()
                .setTime(parser.nextInt(), parser.nextInt(), parser.nextInt(), parser.nextInt());

        position.setValid(parser.next().equals("A"));
        position.setLatitude(parser.nextCoordinate());
        position.setLongitude(parser.nextCoordinate());
        position.setSpeed(parser.nextDouble());
        position.setCourse(parser.nextDouble());

        dateBuilder.setDateReverse(parser.nextInt(), parser.nextInt(), parser.nextInt());
        position.setTime(dateBuilder.getDate());

        position.set(Event.PREFIX_IO + 1, parser.next());

        String odometer = parser.next();
        odometer = odometer.replace(":", "A");
        odometer = odometer.replace(";", "B");
        odometer = odometer.replace("<", "C");
        odometer = odometer.replace("=", "D");
        odometer = odometer.replace(">", "E");
        odometer = odometer.replace("?", "F");
        position.set(Event.KEY_ODOMETER, Integer.parseInt(odometer, 16));

        position.set(Event.KEY_ALARM, parser.next());
        position.set(Event.PREFIX_ADC + 1, parser.next());

        return position;
    }

}
