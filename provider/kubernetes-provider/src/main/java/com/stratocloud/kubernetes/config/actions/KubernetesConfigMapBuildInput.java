package com.stratocloud.kubernetes.config.actions;

import com.stratocloud.form.CodeBlockField;
import com.stratocloud.provider.resource.ResourceActionInput;
import lombok.Data;

@Data
public class KubernetesConfigMapBuildInput implements ResourceActionInput {

    @CodeBlockField(label = "对象定义", language = "yaml", defaultValue = NODE_YAML_EXAMPLE)
    private String yamlContent;


    public static final String NODE_YAML_EXAMPLE = """
            apiVersion: v1
            kind: ConfigMap
            metadata:
              name: game-demo
            data:
              # 类属性键；每一个键都映射到一个简单的值
              player_initial_lives: "3"
              ui_properties_file_name: "user-interface.properties"
            
              # 类文件键
              game.properties: |
                enemy.types=aliens,monsters
                player.maximum-lives=5   \s
              user-interface.properties: |
                color.good=purple
                color.bad=yellow
                allow.textmode=true   \s
            """;
}
