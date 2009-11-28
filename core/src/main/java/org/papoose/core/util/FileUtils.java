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
package org.papoose.core.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.osgi.framework.BundleException;


/**
 * @version $Revision$ $Date$
 */
public class FileUtils
{
    private final static String CLASS_NAME = FileUtils.class.getName();
    private final static Logger LOGGER = Logger.getLogger(CLASS_NAME);

    public static boolean buildDirectoriesFromFilePath(File root, String path, char regex)
    {
        int lastIndex = Math.max(0, path.lastIndexOf(regex));

        return buildDirectories(root, path.substring(0, lastIndex), regex);
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

    public static boolean delete(File file)
    {
        LOGGER.entering(CLASS_NAME, "delete", file);

        boolean result = true;

        try
        {
            if (!isSymlink(file) && file.isDirectory())
            {
                if (LOGGER.isLoggable(Level.FINE)) LOGGER.fine(file + " is a directory");
                for (File f : file.listFiles()) result &= delete(f);
            }
        }
        catch (IOException e)
        {
            LOGGER.log(Level.WARNING, "Unable to test if file is symlink: " + file, e);
            result = false;
        }

        result &= file.delete();

        LOGGER.exiting(CLASS_NAME, "delete", result);

        return result;
    }

    public static InputStream safeStream(File file) throws BundleException
    {
        try
        {
            return new FileInputStream(file);
        }
        catch (FileNotFoundException ioe)
        {
            throw new BundleException("Unable to obtain input stream", ioe);
        }
    }

    /**
     * Determines whether the specified file is a link rather than an actual file.
     * Will not return true if there is a symlink anywhere in the path, only if the specific
     * file is.
     *
     * @param file the file to check
     * @return true iff the file is a symlink
     * @throws IOException if an IO error occurs while checking the file
     */
    public static boolean isSymlink(File file) throws IOException
    {
        if (file == null) throw new IllegalArgumentException("File must not be null");

        File fileInCanonicalDir;
        if (file.getParent() == null)
        {
            fileInCanonicalDir = file;
        }
        else
        {
            File canonicalDir = file.getParentFile().getCanonicalFile();
            fileInCanonicalDir = new File(canonicalDir, file.getName());
        }

        return !fileInCanonicalDir.getCanonicalFile().equals(fileInCanonicalDir.getAbsoluteFile());
    }

    private FileUtils() {}
}
