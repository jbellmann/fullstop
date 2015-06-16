package org.zalando.stups.fullstop.plugin; /**
 * Copyright (C) 2015 Zalando SE (http://tech.zalando.com)
 * <p/>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import com.amazonaws.services.cloudtrail.processinglibrary.model.CloudTrailEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.io.FileReader;
import java.io.IOException;

/**
 * Created by mrandi.
 */
@Component
public class HelloWorldJavascriptPlugin extends AbstractFullstopPlugin {

    public static final String NASHORN = "nashorn";

    private static final Logger LOG = LoggerFactory.getLogger(HelloWorldJavascriptPlugin.class);

    ScriptEngine engine = new ScriptEngineManager().getEngineByName(NASHORN);


    @Override
    public boolean supports(CloudTrailEvent delimiter) {
        return true;
    }

    @Override
    public void processEvent(CloudTrailEvent event) {

        Object result = null;
        String jsScript = ResourceLoader.class.getResource("/js/plugins/fullstop-hello-world/plugin.js").getPath();

        try {
            engine.eval(new FileReader(jsScript));

            Invocable invocable = (Invocable) engine;

            result = invocable.invokeFunction("processEvent", event);
        }
        catch (ScriptException | NoSuchMethodException | IOException e) {
            e.printStackTrace();
        }

        LOG.info("Hello World Javascript plugin result: {}",result);
    }
}
