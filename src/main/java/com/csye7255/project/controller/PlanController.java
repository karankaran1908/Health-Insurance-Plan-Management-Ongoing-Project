package com.csye7255.project.controller;


import com.csye7255.project.Etag.EtagMap;
import com.csye7255.project.Exception.*;
import com.csye7255.project.service.PlanService;
import com.csye7255.project.service.TokenService;
import org.apache.http.HttpHost;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.everit.json.schema.Schema;
import org.everit.json.schema.ValidationException;
import org.everit.json.schema.loader.SchemaLoader;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import java.io.BufferedReader;
import java.io.FileReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping
public class PlanController {

    @Autowired
    private PlanService planService;

    @Autowired
    private TokenService tokenService;

    @Value("${spring.redis.host}")
    String a;

    private static JedisPool jedisPool = new JedisPool("localhost", 6379);
    private RestHighLevelClient restHighLevelClient = new RestHighLevelClient(RestClient.builder(new HttpHost("localhost", 9200)).setRequestConfigCallback(requestConfigBuilder ->
            requestConfigBuilder
                    .setConnectTimeout(5000)
                    .setSocketTimeout(5000))
            .setMaxRetryTimeoutMillis(60000));
    @PostMapping(path = "/plan", consumes = "application/json", produces = "application/json")
    @ResponseBody
    public ResponseEntity<String> addInsurancePlan(@RequestHeader HttpHeaders headers,@RequestBody String plan) {
        String token = headers.getFirst("Authorization");
        Map<String,String> validEtag = planService.createPlan(token,plan);
        String etag = validEtag.get("etag") ;
        System.out.println(etag);

        if(validEtag.size()>1){
            return ResponseEntity.status(HttpStatus.CREATED).eTag(etag).body("Data Saved. PlanId: " + validEtag.get("planid"));
        }
        else {
            return ResponseEntity.status(HttpStatus.ALREADY_REPORTED).eTag(etag).body("Data already present");
        }
    }

    @GetMapping(path = "/plan/{id}", produces = "application/json")
    public ResponseEntity<String> getInsurancePlan(@PathVariable(value = "id") String planId, @RequestHeader HttpHeaders header) {
        try {
            String token = header.getFirst("Authorization");
            if (planId != null) {
                String etag = header.getETag();

                Map<String, String> validEtag = planService.getPlan(token, planId, etag);

                return ResponseEntity.status(HttpStatus.OK).eTag(validEtag.get("etag")).body(validEtag.get("plan"));
            } else {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).eTag(planService.getEtags(planId)).body("PlanId not present");
            }
        }catch(BadRequest ex){
            return ResponseEntity.status(ex.getStatus()).eTag(planService.getEtags(planId)).body(ex.getMessage());
        }
        catch (Forbidden ex){
            return ResponseEntity.status(ex.getStatus()).eTag(planService.getEtags(planId)).body(ex.getMessage());
        }
        catch (PreconditionFailed ex){
            return ResponseEntity.status(ex.getStatus()).eTag(planService.getEtags(planId)).body(ex.getMessage());
        }
        catch (Notmodified ex){
            return ResponseEntity.status(ex.getStatus()).eTag(planService.getEtags(planId)).body(ex.getMessage());
        }
    }


    @DeleteMapping(path = "/plan/{id}")
    public ResponseEntity<String> deletePlan(@PathVariable(value = "id") String planId, @RequestHeader HttpHeaders headers) {
        String token = headers.getFirst("Authorization");
        if(!(tokenService.validateToken(token))) throw new BadRequest("Token is expired");
        if (planId != null) {
            if ((new PlanService().deleteData(planId)) > 0) {
                planService.removeEtags(planId);
                return ResponseEntity.status(HttpStatus.OK).body("Delete Successful!");
            } else
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Plan Id Not Found");
        } else {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Enter the plan ID!!");
        }
    }


    @PutMapping(path = "/plan/{id}")
    public ResponseEntity<String> updatePlan(@PathVariable(value = "id") String planId, @RequestBody String plan, @RequestHeader HttpHeaders header) {
        try {
            String token = header.getFirst("Authorization");
            String etag = null;
            if (header.getIfMatch().get(0) != null) {
                etag = header.getIfMatch().get(0);
                etag = etag.replace("\"", "");
            }
            System.out.println(etag);
            Map<String, String> validEtag = planService.updatePlan(token, planId, etag, plan);
            etag = validEtag.get("etag");
            if (validEtag.size() > 1) {
                return ResponseEntity.status(HttpStatus.OK).eTag(etag).body("Data Saved. Plan id:" + validEtag.get("planid"));
            } else {
                return ResponseEntity.status(HttpStatus.NOT_MODIFIED).eTag(etag).body("Data not updated");
            }
        }catch(BadRequest ex){
            return ResponseEntity.status(ex.getStatus()).eTag(planService.getEtags(planId)).body(ex.getMessage());
        }
        catch (Forbidden ex){
            return ResponseEntity.status(ex.getStatus()).eTag(planService.getEtags(planId)).body(ex.getMessage());
        }
        catch (PreconditionFailed ex){
            return ResponseEntity.status(ex.getStatus()).eTag(planService.getEtags(planId)).body(ex.getMessage());
        }
        catch (Notmodified ex){
            return ResponseEntity.status(ex.getStatus()).eTag(planService.getEtags(planId)).body(ex.getMessage());
        }

    }

    @PatchMapping(path = "/plan/{id}", produces = "application/json")
    public ResponseEntity<String> patchPlan1(@PathVariable(value = "id") String planId, @RequestBody String plan,@RequestHeader HttpHeaders headers) throws Exception {
        Jedis jedis = jedisPool.getResource();
        try {
            String etag = null;

            String token = headers.getFirst("Authorization");
            if (!(tokenService.validateToken(token))) throw new BadRequest("Token is expired");
            if (headers.getIfMatch().size() > 0) {
                if (headers.getIfMatch().get(0) != null) {
                    etag = headers.getIfMatch().get(0);
                    etag = etag.replace("\"", "");
                }
            } else {
                throw new Forbidden("ETag is not present");
            }

            if (EtagMap.getEtags().containsKey(planId + "p")) {
                if (!etag.equals(EtagMap.getEtags().get(planId + "p")))
                    throw new Forbidden("ETag not matched");
            }
            JSONObject input = new JSONObject(plan);


            if (!planId.contains("plan_") || !input.keySet().contains("objectId")) {
                JSONObject data = new JSONObject(jedis.get(planId));
                for (Object key : input.keySet()) {
                    if (!data.get((String) key).equals(input.get((String) key))) {
                        data.put(key.toString(), input.get((String) key));
                    }
                }
                jedis.set(planId, data.toString());

                String skey = null;
                Set<String> set = EtagMap.getEtags().keySet()
                        .stream()
                        .filter(s -> s.endsWith("p"))
                        .collect(Collectors.toSet());
                if (!set.isEmpty()) {
                    EtagMap.getEtags().keySet().removeAll(set);
                    for (String s : set) {
                        skey = s;
                    }
                }

                etag = UUID.randomUUID().toString();
                EtagMap.getEtags().put(skey, etag);

            } else {
                if (input.getString("objectType").equalsIgnoreCase("planservice")) {
                    String message = validateJson1(input);
                    if (message != null) {
                        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Schema not validated. Please check " + message);
                    }
                    Map<String, String> data = PlanService.retrieveMap(input);
                    for (Map.Entry entry : data.entrySet()) {
                        jedis.set((String) entry.getKey(), (String) entry.getValue());
                    }
                    String fullPlan = jedis.get(planId);

                    System.out.println(fullPlan + "\n\n\n");
                    String[] split = fullPlan.split("\\[");
                    String x = "[\\\"" + input.getString("objectType") + "_" + input.getString("objectId") + "\\\",";
                    fullPlan = split[0] + x + split[1];
                    jedis.set(planId, fullPlan);
                    planService.removeEtags(planId);
                    etag = UUID.randomUUID().toString();
                    EtagMap.getEtags().put(planId + "p", etag);
                    System.out.println(new PlanService().readData(fullPlan).toString() + "\n xxx" + planId.split("_")[1]);
                    //  restHighLevelClient.delete(new DeleteRequest("plan_index", "plan", planId.split("_")[1]));
                    //restHighLevelClient.close();
                    String IndexQueue = "RedisIndexQueue";
                    jedis.rpush(IndexQueue.getBytes(), new PlanService().readData(fullPlan).toString().getBytes(StandardCharsets.UTF_8));
                    jedis.close();
                }

            }
            jedis.close();

            return ResponseEntity.status(HttpStatus.OK).eTag(etag).body("Data Saved. Plan id: " + planId);
        }catch(BadRequest ex){
        return ResponseEntity.status(ex.getStatus()).eTag(planService.getEtags(planId)).body(ex.getMessage());
    }
        catch (Forbidden ex){
        return ResponseEntity.status(ex.getStatus()).eTag(planService.getEtags(planId)).body(ex.getMessage());
    }
        catch (PreconditionFailed ex){
        return ResponseEntity.status(ex.getStatus()).eTag(planService.getEtags(planId)).body(ex.getMessage());
    }
        catch (Notmodified ex){
        return ResponseEntity.status(ex.getStatus()).eTag(planService.getEtags(planId)).body(ex.getMessage());
    }
        finally{
            if (jedis != null)
            jedis.close();
        }
    }


    public String validateJson1(JSONObject jsonData) throws Exception{
        BufferedReader bufferedReader = new BufferedReader(new FileReader("src/main/resources/static/Service.json"));
        JSONObject jsonSchema = new JSONObject(
                new JSONTokener(bufferedReader));
        System.out.println(jsonSchema.toString());
        Schema schema = SchemaLoader.load(jsonSchema);
        try {
            schema.validate(jsonData);
            return null;
        } catch (ValidationException e) {
            //return ResponseEntity.status(e.getStatus()).eTag(planService.getEtags(planId)).body(ex.getMessage());
            throw new ExpectationFailed("Enter correct input! The issue is present in " + e.getMessage());
        }
    }
}
