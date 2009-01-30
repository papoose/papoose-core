/**
 *
 * Copyright 2008-2009 (C) The original author or authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.papoose.core.framework.util;

import java.io.File;


/**
 * @version $Revision$ $Date$
 */
public class FileUtils
{
    public static boolean buildDirectoriesFromFilePath(File root, String path, char regex)
    {
        return buildDirectories(root, path.substring(0, path.indexOf(regex)), regex);
    }

    public static boolean buildDirectories(File root, String path, char regex)
    {
        for (String directory : path.split(String.valueOf(regex))) root = new File(root, directory);
        return root.mkdirs();
    }

    public static File buildPath(File root, Object... elements)
    {
        for (Object element : elements) root = new File(root, element.toString());
        return root;
    }

    private FileUtils() {}
}
