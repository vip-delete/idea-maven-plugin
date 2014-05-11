package com.github.ideaplugin;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Vasiliy Zhukov
 * @see org.codehaus.plexus.util.xml.PrettyPrintXMLWriter
 * @since 08/07/2010
 */
class XMLUtil {
    private static String escapeXml(String text) {
        text = text.replaceAll("&", "&amp;");

        text = text.replaceAll("<", "&lt;");

        text = text.replaceAll(">", "&gt;");

        text = text.replaceAll("\"", "&quot;");

        text = text.replaceAll("\'", "&apos;");

        return text;
    }

    public static String escapeXmlAttribute(String text) {
        text = escapeXml(text);

        // Windows
        text = text.replaceAll("\r\n", "&#10;");

        Pattern pattern = Pattern.compile("([\000-\037])");
        Matcher m = pattern.matcher(text);
        StringBuffer b = new StringBuffer();
        while (m.find()) {
            m = m.appendReplacement(b, "&#" + Integer.toString(m.group(1).charAt(0)) + ";");
        }
        m.appendTail(b);

        return b.toString();
    }
}
