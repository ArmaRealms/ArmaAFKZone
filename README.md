**Bug Reports and Feature Requests:** https://github.com/Artillex-Studios/Issues

**Support:** https://dc.artillex-studios.com/

## Integração com addons: recompensas recebidas

O evento Bukkit `com.artillexstudios.axafkzone.api.events.PlayerRewardEvent`
é disparado uma vez por `Reward` entregue, após despachar seus comandos e
adicionar seus itens ao inventário (ou soltá-los no chão quando não há espaço).
Se um ciclo sortear vários prêmios, haverá um evento para cada entrega, inclusive
quando o mesmo prêmio for sorteado novamente. O evento não é cancelável e não
depende das mensagens de recompensa estarem habilitadas.

Compile o addon com o JAR do AxAFKZone como dependência `provided` (ou
`compileOnly` no Gradle), sem incluí-lo no JAR do addon. Declare no `plugin.yml`:

```yaml
depend: [AxAFKZone]
```

Exemplo de listener no addon:

```java
import com.artillexstudios.axafkzone.api.events.PlayerRewardEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

public final class RewardLogger extends JavaPlugin implements Listener {
    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onReward(PlayerRewardEvent event) {
        String zone = event.getZone() == null ? "direto" : event.getZone().getName();
        getLogger().info(event.getPlayer().getUniqueId() + " ganhou "
                + event.getReward().getDisplay() + " na zona " + zone);
    }
}
```

`getPlayer()` identifica o jogador; `getReward()` expõe a configuração do prêmio
(display, itens, comandos, chance e requisitos); `getZone()` informa sua origem.
A chamada existente `Reward.run(player)` também dispara o evento, com zona `null`.
O display é opcional e não é um identificador único. Não altere a configuração
retornada pelo evento; copie os dados e clone os itens que precisar guardar.

O listener roda na thread principal no Bukkit/Paper e no scheduler do jogador
no Folia. Para persistir em banco de dados, copie os dados necessários no listener
e faça a gravação de forma assíncrona. O evento confirma a entrega pelo AxAFKZone;
não confirma o sucesso interno ou efeitos assíncronos de comandos de outros plugins.
Se o jogador sair antes da tarefa de entrega, ela poderá ser descartada sem evento.

![axafkzone-banner](https://github.com/user-attachments/assets/8ece312c-c27e-4899-be85-1c69836f65f0)
