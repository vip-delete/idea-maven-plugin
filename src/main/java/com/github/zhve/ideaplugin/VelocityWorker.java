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

import org.apache.velocity.Template;
import org.apache.velocity.app.Velocity;
import org.apache.velocity.runtime.RuntimeConstants;

/**
 * Init velocity and load templates
 *
 * @author Vasiliy Zhukov
 * @since 07/27/2010
 */
class VelocityWorker
{
    private Template imlTemplate;
    private Template iprTemplate;
    private Template iwsTemplate;

    public VelocityWorker() throws Exception
    {
        Velocity.addProperty("resource.loader", "class");
        Velocity.addProperty("class.resource.loader.class", "org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader");
        Velocity.addProperty(RuntimeConstants.RUNTIME_LOG, System.getProperty("java.io.tmpdir") + "/velocity.log");
        Velocity.init();

        imlTemplate = Velocity.getTemplate("ideaplugin/idea-iml.vm");
        iprTemplate = Velocity.getTemplate("ideaplugin/idea-ipr.vm");
        iwsTemplate = Velocity.getTemplate("ideaplugin/idea-iws.vm");
    }

    // Getters

    public Template getImlTemplate()
    {
        return imlTemplate;
    }

    public Template getIprTemplate()
    {
        return iprTemplate;
    }

    public Template getIwsTemplate()
    {
        return iwsTemplate;
    }
}
