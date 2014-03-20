/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ru.inversion.webstartplugin;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Formatter;
import java.util.List;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @goal webstart
 */
public class WebStartPlugin extends AbstractMojo {

    /**
     * @parameter @required
     */
    private String workingDirectory;

    /**
     * @parameter default-value="launch.jnlp"
     * @required
     */
    private String jnlpFileName;

    /**
     * @parameter
     */
    private String codeBase;

    /**
     * @parameter default-value="title"
     */
    private String title;

    /**
     * @parameter default-value="vendor"
     */
    private String vendor;

    /**
     * @parameter
     */
    private String iconHref;

    /**
     * @parameter
     */
    private String mainJarName;

    /**
     * @parameter @required
     */
    private String mainClass;

    /**
     * @parameter
     */
    private List arguments;

    /**
     * @parameter
     */
    private String copyTo;

    @Override
    public void execute() throws MojoExecutionException {
        try {

            launchSignerJars();
            String jnlp = generateJnlpFile();
            writeJnlpToFile(jnlp);
            if (copyTo != null && !copyTo.isEmpty()) {

                Thread.sleep(5000);
                copyDirectory(new File(workingDirectory), new File(copyTo));
            }
        } catch (IOException ex) {
            throw new MojoExecutionException("Error when executing WebStartPluging", ex);
        } catch (InterruptedException ex) {

        }
    }

    private void launchSignerJars() throws IOException {
        workingDirectory = workingDirectory.replace("\\", "/");
        System.out.println(workingDirectory);
        
        Process p = Runtime.getRuntime().exec("cmd");
        PrintWriter stdin = new PrintWriter(p.getOutputStream());
        stdin.println("cd " + workingDirectory);
        stdin.println("Signer.js");
        stdin.close();
    }

    private String generateJnlpFile() throws IOException {

        String jnlpFileString = "";
        StringBuilder builder = new StringBuilder();
        builder.append("<?xml version=\"1.0\" encoding=\"utf-8\"?> \n");

        builder.append("<jnlp spec=\"1.0+\" ");
        if (codeBase != null && !codeBase.isEmpty()) {
            builder.append("codebase=\"").append(codeBase).append("\" ");
        }
        builder.append("href=\"").append(jnlpFileName).append(".jnlp\">\n");

        builder.append("<information>\n");
        builder.append("<title>").append(title).append("</title>\n");
        builder.append("<vendor>").append(vendor).append("</vendor>\n");
        if (iconHref != null && !iconHref.isEmpty()) {
            builder.append("<icon href=\"").append(iconHref).append("\" kind=\"splash\"/>\n");
        }
        builder.append("</information>\n");

        builder.append("	<security>\n"
                + "		<all-permissions/>\n"
                + "	</security>");

        builder.append("<resources>\n");
        for (String jarName : getListJarFiles()) {
            builder.append("<jar href=\"").append(jarName);
            if (jarName.equals(mainJarName)) {
                builder.append("\" main=\"true\"/>\n");
            } else {
                builder.append("\" />\n");
            }
        }
        builder.append("</resources>\n");

        builder.append("<application-desc main-class=\"").append(mainClass).append("\">\n");
        if (arguments != null && !arguments.isEmpty()) {
            for (Object arg : arguments) {
                builder.append("<argument>").append((String) arg).append("</argument>\n");
            }
        }
        builder.append("</application-desc>\n");

        builder.append("</jnlp>\n");
        jnlpFileString = builder.toString();
        return jnlpFileString;
    }

    private void writeJnlpToFile(String jnlpString) throws IOException {

        System.out.println("______________________________________Writing_______________________________________________");
        System.out.println(jnlpString);

        Formatter frm = null;
        try {
            String jnlpPath = workingDirectory + "/" + jnlpFileName + ".jnlp";
            System.out.println(jnlpPath);
            File f = new File(jnlpPath);

            f.createNewFile();
            frm = new Formatter(f);
            frm.format(jnlpString);
        } finally {
            if (frm != null) {
                frm.flush();
                frm.close();
            }
        }
    }

    private List<String> getListJarFiles() {

        List<String> result = new ArrayList<>();
        File directory = new File(workingDirectory);
        File[] filesJar = directory.listFiles(new FileFilter() {

            @Override
            public boolean accept(File pathname) {
                if (pathname.isFile()) {
                    String name = pathname.getAbsolutePath().toLowerCase();
                    if (name.endsWith("jar")) {
                        return true;
                    } else {
                        return false;
                    }
                } else {
                    return false;
                }
            }
        });

        for (File file : filesJar) {
            result.add(file.getName());
        }

        return result;
    }

    private void copyDirectory(File sourceLocation, File targetLocation)
            throws IOException {

        if (sourceLocation.isDirectory()) {
            if (!targetLocation.exists()) {
                targetLocation.mkdir();
            }

            String[] children = sourceLocation.list();
            for (int i = 0; i < children.length; i++) {
                copyDirectory(new File(sourceLocation, children[i]),
                        new File(targetLocation, children[i]));
            }
        } else {

            InputStream in = new FileInputStream(sourceLocation);
            OutputStream out = new FileOutputStream(targetLocation);

            // Copy the bits from instream to outstream
            byte[] buf = new byte[1024];
            int len;
            while ((len = in.read(buf)) > 0) {
                out.write(buf, 0, len);
            }
            in.close();
            out.close();
        }
    }
}
