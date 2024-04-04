package org.ktximageio.ktx;

import java.nio.file.Paths;

import org.ktximageio.output.TonemappWindow.WindowListener;

public class Test implements WindowListener {

    static String getPath(String filename) {
        return Paths.get(filename).isAbsolute() ? filename : "src/test/resources/" + filename;
    }

    @Override
    public boolean windowEvent(WindowEvent event) {
        System.out.println("Event: " + event.action);
        return true;
    }

}
