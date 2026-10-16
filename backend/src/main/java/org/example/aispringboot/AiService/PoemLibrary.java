package org.example.aispringboot.AiService;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 温暖诗句库
 * 在用户情绪低落时，从中随机挑选一首，随安慰邮件一起发送。
 */
public final class PoemLibrary {

    private PoemLibrary() {
    }

    /**
     * @param content   诗句
     * @param author    作者
     * @param warmWords 一句温和的安抚语
     */
    public record Poem(String content, String author, String warmWords) {
    }

    public static final List<Poem> POEMS = List.of(
            new Poem("山重水复疑无路，柳暗花明又一村。", "陆游",
                    "此刻的迷雾终会散去，别急着否定自己。"),
            new Poem("沉舟侧畔千帆过，病树前头万木春。", "刘禹锡",
                    "冬天再长，春天也一定会来。"),
            new Poem("长风破浪会有时，直挂云帆济沧海。", "李白",
                    "属于你的风浪终会被你穿过。"),
            new Poem("苔花如米小，也学牡丹开。", "袁枚",
                    "再微小的你，也有努力盛开的权利。"),
            new Poem("行到水穷处，坐看云起时。", "王维",
                    "走不动的时候，不妨先停下来，看看云。"),
            new Poem("竹杖芒鞋轻胜马，谁怕？一蓑烟雨任平生。", "苏轼",
                    "愿你也能对自己说一句：没关系，我可以慢慢来。"),
            new Poem("莫愁前路无知己，天下谁人不识君。", "高适",
                    "你并不孤单，总有人愿意听你说说话。"),
            new Poem("海内存知己，天涯若比邻。", "王勃",
                    "关心你的人，或许比你想象的多。"),
            new Poem("春有百花秋有月，夏有凉风冬有雪。", "释绍昙",
                    "愿你心里少一些挂碍，多一些此刻的安宁。"),
            new Poem("野火烧不尽，春风吹又生。", "白居易",
                    "你比自己以为的更有韧性。"),
            new Poem("不畏浮云遮望眼，自缘身在最高层。", "王安石",
                    "别被一时的阴云，遮住了你本来的光。"),
            new Poem("总把新桃换旧符，愿君日日是好日。", "王安石（化用）",
                    "愿你今晚好好睡一觉，明天是新的一天。")
    );

    /**
     * 随机取一首
     */
    public static Poem random() {
        return POEMS.get(ThreadLocalRandom.current().nextInt(POEMS.size()));
    }
}
