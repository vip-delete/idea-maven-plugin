package com.github.zhve.ideaplugin;

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

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author Vasiliy Zhukov
 * @since 5/31/2014
 */
public class GenerateBat {
    public static void main(String[] args) {
        // generate idea.bat
        List<String> list = new ArrayList<String>();
        for (Field f : IdeaPluginMojo.class.getDeclaredFields())
            list.add(f.getName());
        Collections.sort(list);

        System.out.println("@echo off");
        System.out.println("");
        for (String f : list) {
            System.out.println("set " + f + "=");
        }

        System.out.println("\nrem -- Check Command Line --\n" +
                "\n" +
                "if not \"%1\" == \"\" goto ok\n" +
                "echo Usage: idea goal\n" +
                "echo \n" +
                "echo Goals:\n" +
                "echo     idea     Create workspace files\n" +
                "echo     clean    Delete workspace files\n" +
                "echo     help     Show help\n" +
                "echo     list     Show dependencies\n" +
                "exit 0\n" +
                ":ok\n");

        System.out.println("rem -- Concatenate Parameters --\n" +
                "set command = \"\"");

        for (String f : list) {
            System.out.println("\n" +
                    "if \"%" + f + "%\" == \"\" goto " + f + "\n" +
                    "set command=%command% -D" + f + "=\"%" + f + "%\"\n" +
                    ":" + f);
        }

        System.out.println("\n" +
                "@echo on\n" +
                "mvn %command% com.github.zhve:idea-maven-plugin:3.0b1:%1\n");
    }
}
