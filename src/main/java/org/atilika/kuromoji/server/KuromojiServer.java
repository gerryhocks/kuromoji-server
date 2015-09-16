/**
 * Copyright 2010-2015 Atilika Inc. and contributors (see CONTRIBUTORS.md)
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.  A copy of the
 * License is distributed with this work in the LICENSE.md file.  You may
 * also obtain a copy of the License from
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.atilika.kuromoji.server;

import com.atilika.kuromoji.Dictionary;
import com.atilika.kuromoji.Token;
import com.atilika.kuromoji.Tokenizer;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

@Path("/tokenizer")
public class KuromojiServer {

    private static final Logger log = LoggerFactory.getLogger(KuromojiServer.class);

    private static final String DOT_COMMAND = "dot -Tsvg";

    private static final int MAX_INPUT_LENGTH = 512;

    private static final int MAX_INPUT_VITERBI_LENGTH = 32;

    private static final Map<String, Tokenizer> tokenizerCache = new HashMap<String, Tokenizer>();

//    private static DebugTokenizer debugTokenizer = DebugTokenizer.builder().build();

    @GET
    @Path("/tokenize")
    @Produces(MediaType.APPLICATION_JSON)
    public Response tokenizeGet(@QueryParam("text") String text,
                                @DefaultValue("utf-8") @QueryParam("encoding") String encoding,
                                @DefaultValue("0") @QueryParam("mode") int mode)
        throws JSONException, UnsupportedEncodingException {
        return Response.ok(tokenize(text, encoding, mode)).build();
    }


    @POST
    @Path("/tokenize")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response tokenizePost(@FormParam("text") String text,
                                 @DefaultValue("utf-8") @FormParam("encoding") String encoding,
                                 @DefaultValue("0") @FormParam("mode") int mode) throws JSONException, IOException {
        return Response.ok(tokenize(text, encoding, mode)).build();
    }

    private JSONObject tokenize(String text, String encoding, int mode) throws JSONException, UnsupportedEncodingException {
        text = URLDecoder.decode(text, encoding);
        log.info("Tokenizing text " + text + " using mode " + mode);

        if (mode == 3) {
            text = trimInputText(text, MAX_INPUT_VITERBI_LENGTH);
        } else {
            text = trimInputText(text, MAX_INPUT_LENGTH);
        }

        return createResponse(text, mode);
    }

    private String trimInputText(String text, int maxLength) {
        int length = text.length();
        if (length > maxLength) {
            text = text.substring(0, maxLength);
            log.warn("Input length " + length + " exceeds max length.  Trimming to max length of " + maxLength);
        }
        return text;
    }

    private JSONObject createResponse(String text, int mode) throws JSONException {

        JSONObject json = new JSONObject();
        JSONObject results = new JSONObject();

        for (String dictionaryName : Dictionary.getDictionaryNames()) {
            try {
                long now = System.currentTimeMillis();
                Tokenizer tokenizer = getTokenizer(dictionaryName);
                List<Token> tokens = tokenizer.tokenize(text);
                System.err.println("Tokenize: " + (System.currentTimeMillis() - now) + "ms");

                JSONArray jsonTokens = new JSONArray();
                results.put(dictionaryName, jsonTokens);
                for (Token token : tokens) {
                    JSONObject jsonToken = new JSONObject();
                    jsonToken.put("SURFACE", token.getSurface());
                    for (String name : token.getFeatureNames()) {
                        jsonToken.put(name, token.getFeature(name));
                    }
                    jsonTokens.put(jsonToken);
                }
            } catch (Exception e) {
                throw new JSONException("Exception when tokenizing");
            }
        }

        json.put("tokens", results);
        json.put("input", text);
        json.put("mode", mode);

        if (mode == 3) {
//            json.put("viterbi", getViterbiSVG(debugTokenizer.debugTokenize(text)));
        }
        return json;
    }

    private Tokenizer getTokenizer(String dictionaryName) {
        Tokenizer tokenizer = tokenizerCache.get(dictionaryName);
        if (tokenizer == null) {
            try {
                tokenizer = new Tokenizer(dictionaryName);
                tokenizerCache.put(dictionaryName, tokenizer);
            } catch (Exception e) {
                log.error("Failed to create Tokenizer", e);
            }
        }
        return tokenizer;
    }

    private String getViterbiSVG(String dot) {
        Process process = null;
        try {
            log.info("Running " + DOT_COMMAND);
            process = Runtime.getRuntime().exec(DOT_COMMAND);
            process.getOutputStream().write(dot.getBytes("utf-8"));
            process.getOutputStream().close();

            InputStream input = process.getInputStream();
            String svg = new Scanner(input, "utf-8").useDelimiter("\\A").next();

            int exitValue = process.exitValue();

            log.debug("Read " + svg.getBytes("utf-8").length + " bytes of SVG output");
            log.info("Process exited with exit value " + exitValue);
            return svg;
        } catch (IOException e) {
            log.error("Error running process " + process, e.getCause());
            return null;
        } finally {
            if (process != null) {
                log.info("Destroying process " + process);
                process.destroy();
            }
        }
    }
}