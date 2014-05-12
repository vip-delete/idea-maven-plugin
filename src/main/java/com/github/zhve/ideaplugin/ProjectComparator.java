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

import org.apache.maven.project.MavenProject;

import java.util.Comparator;

/**
 * @author Vasiliy Zhukov
 * @since 05/14/2011
 */
class ProjectComparator implements Comparator<MavenProject>
{
    public static final ProjectComparator INSTANCE = new ProjectComparator();

    @Override
    public int compare(MavenProject o1, MavenProject o2)
    {
        return o1.getArtifactId().compareTo(o2.getArtifactId());
    }
}
