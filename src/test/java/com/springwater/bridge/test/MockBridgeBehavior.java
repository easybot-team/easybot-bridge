package com.springwater.bridge.test;

import com.springwater.easybot.bridge.BridgeBehavior;
import com.springwater.easybot.bridge.ClientProfile;
import com.springwater.easybot.bridge.message.Segment;
import com.springwater.easybot.bridge.model.PlayerInfo;
import com.springwater.easybot.bridge.model.ServerInfo;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class MockBridgeBehavior implements BridgeBehavior {

    MockBridgeBehavior() {
        ClientProfile.setPluginVersion("1.0.0");
        ClientProfile.setServerDescription("junit");
    }

    @Override
    public String runCommand(String playerName, String command, boolean enablePapi) {
        return "MOCK RESULT";
    }

    @Override
    public String papiQuery(String playerName, String query) {
        return query;
    }

    @Override
    public ServerInfo getInfo() {
        ServerInfo info = new ServerInfo();
        info.setPapiSupported(true);
        info.setServerVersion("mock");
        info.setPluginVersion("1.0.0");
        info.setServerName("mock@junit");
        info.setCommandSupported(true);
        return info;
    }

    @Override
    public void SyncToChat(String message) {

    }

    @Override
    public void BindSuccessBroadcast(String playerName, String accountId, String accountName) {

    }

    @Override
    public void KickPlayer(String plauer, String kickMessage) {

    }

    @Override
    public void SyncToChatExtra(List<Segment> segments, String text) {

    }


    private static final String[] PREDEFINED_SKINS = {
            "https://skin.mclists.cn/skin/skins/dea93d900f05d46a4f471241f4258d3b09e1015bbeb0c051972dd42a4ff8b294.png",
            "https://skin.mclists.cn/skin/skins/153412f1678fafbcf0ede73575eda2ff826c17cb40d04e9cae6ff41d3ebf2a92.png",
            "https://skin.mclists.cn/skin/skins/3ae2006e822564f0c69569864804da224d8c7eff447aaa590ded6032192e0342.png",
            "https://skin.mclists.cn/skin/skins/66461a9bc0ff136fab8f74c38bacbbbb6cd561d9dd3795e3aeee906bd6547a1f.png",
            "https://skin.mclists.cn/skin/skins/4f9d49954af380d627b83a89dba708ec619e520d42583d9a2a83fa73193405c4.png",
            "https://skin.mclists.cn/skin/skins/ac8b92d88f5d839a2eb5840742f0e7b8e5c366a57fa0a5d16496fed96148a051.png",
            "https://skin.mclists.cn/skin/skins/45f1630458fee92f2aa3901942a805b63708a505cc234fbf0fbadee3da83a7cd.png",
            "https://skin.mclists.cn/skin/skins/19530628c9412b11051ca169a3e15cff15530666c6fc299e949e371c901f6740.png",
            "https://skin.mclists.cn/skin/skins/e25f00bba5afffdd65522dd58cb08d55349376bca53f8f6dbeedea6e7246ae68.png",
            "https://skin.mclists.cn/skin/skins/4e6b5a504bf8efc98e2d1d3372492b5c7b9e5dae42cf70327f66f9ba59aeac21.png",
            "https://skin.mclists.cn/skin/skins/a4464b28b048b46aba8f3008719befede0379c095ff70a6b336b07112bded183.png",
            "https://skin.mclists.cn/skin/skins/af324f29fe5a40a59a170ffcc257a24e862cbafa3398146a02f3b95a7a52da58.png",
            "https://skin.mclists.cn/skin/skins/1ff5c9aba046a45412e7f34e1b70d31a3528c8bc6ad0a8fd1c2c1aba9da3b2ae.png",
            "https://skin.mclists.cn/skin/skins/d9a8fa299aaeef1306d33df8164ef0f2a922a9b73d3588b629af6288974914a.png",
            "https://skin.mclists.cn/skin/skins/779c12303c957fd61d7726bda512f6e138620891b324fab1d388eba15ceec85.png",
            "https://skin.mclists.cn/skin/skins/f238c089f37a99ed0e7e4f573cdb5c30997e1e8f5dd633cdd4050449f6c670a5.png",
            "https://skin.mclists.cn/skin/skins/a4a43b2b292b17e73b97ca6480fbc0aac61d8dde602a13986915f6f858e2beff.png",
            "https://skin.mclists.cn/skin/skins/49945657b4cfa30cea87ba4660ac49f3792dc065dcde17f86ef1930ca34c6b80.png",
            "https://skin.mclists.cn/skin/skins/c3f19b16cfb7bac818e85e76b87e244c5fad625f141dbadbfd2e23b36d071dbb.png",
            "https://skin.mclists.cn/skin/skins/f359d767742b9135522d094d4a6431a7e491d5731080d182a0ffcfc2d3fa3b81.png",
            "https://skin.mclists.cn/skin/skins/cf4094afb7a8f18371575b8e66b6735f5540f2c2182a6aded57509f9bf6d9676.png",
            "https://skin.mclists.cn/skin/skins/4ba5d4f44034cc2116ca00f40366ce4dc1fe7a96f0ef755a05422df02dc3574e.png",
            "https://skin.mclists.cn/skin/skins/84b743e2bfab7a15a3953b818d99922225a79de5e2010c09c79cd314b5fbc1c3.png",
            "https://skin.mclists.cn/skin/skins/a19ffe1379e72bb07480b94e8ebd331466a94565c59cc01802eae7770280ac5.png",
            "https://skin.mclists.cn/skin/skins/faaf3292488c11702c5bb9765a18b959348d454f5cb28c4ce3c57e815b8df308.png",
            "https://skin.mclists.cn/skin/skins/870f268bf2620742af60329e3dfb7d9f781a2e183724ab5fd4dd77bf425de7c7.png",
            "https://skin.mclists.cn/skin/skins/bed099416323172c9d1ff832e1ae17e4ba55fe00fbd0eeb0326f9cfbc6d5949.png",
            "https://skin.mclists.cn/skin/skins/82a65365d7a14bd0953a4d37089412dc87739f9ec3b29f125537feea3226598c.png",
            "https://skin.mclists.cn/skin/skins/ff8d0bd5bdcd6fa5ad5e3560e15c2b71004443f333c73416fb5912dfe3f31d75.png",
            "https://skin.mclists.cn/skin/skins/7682a90903a36351aec3d684e24e7ba4dd50ef08e2cf2c9fa7a01c686f0d1d60.png",
            "https://skin.mclists.cn/skin/skins/2c050c4536c16c371e2d53440ba3209f5f3aa6e3079796d02ca3652f44585805.png",
            "https://skin.mclists.cn/skin/skins/24228cea455b1b46525a457ea1618bceb7b761d8817c8dc49fe1fe6125ad566e.png",
            "https://skin.mclists.cn/skin/skins/b353f01027f4e0595632fa2348759593d6ec19246106c864d5fdfa6faebb385d.png",
            "https://skin.mclists.cn/skin/skins/3a0087d7d984266c19fcc84f9eb790db2cf21c8f50ebce06736f5dd526e8b0ce.png",
            "https://skin.mclists.cn/skin/skins/ee68f68a69bf335765e238267525e88a9ddbeb47a099e76be763e6abcfcdc21f.png",
            "https://skin.mclists.cn/skin/skins/b1eb983b99884fd8d0649e6877570e9bd123712a75c88a3fa11146ba29d02e1d.png",
            "https://skin.mclists.cn/skin/skins/1fb001e29efab02d0b046dda0f2d26d118c500f7546bb684ad03b51ac832edca.png",
            "https://skin.mclists.cn/skin/skins/46ad1295e8f484fb94959af8f800757f7cc6bb2357ab71d17ce1a8b7616620de.png",
            "https://skin.mclists.cn/skin/skins/6ef05db4dcae9b628554afdaba08f1ca40b19eb83b37d33fb31b979c2c956833.png",
            "https://skin.mclists.cn/skin/skins/1aa41f27cabfd88095e8517d04c502b440208f488f5ea01df0755a8e7165e0b7.png",
            "https://skin.mclists.cn/skin/skins/ee59eed93ed58128f2e7a2008ea0ca56b6b95526803de1332a636054a05c057f.png",
            "https://skin.mclists.cn/skin/skins/4266c22fa7b643fdcd619283be99f8e0a03783af205dfdda3bfd7f29e6f8a651.png",
            "https://skin.mclists.cn/skin/skins/a6df1b7d6b3c866331b34e06cd8841429a017b3f599a632719cf5fdd24ae39ad.png",
            "https://skin.mclists.cn/skin/skins/710b9eda730738b179a6f87c20d8cc816e17e0d0ece287bf6c506bf387ee7362.png",
            "https://skin.mclists.cn/skin/skins/3069b01a64a8b143ba60d7c70d578029f02d58d00da48fa349d21a6c7e935493.png",
            "https://skin.mclists.cn/skin/skins/c8068a277dc2cc3c4ffa647c7433273ffba93ff8a5339643b5ca9e0ddf3bec44.png",
            "https://skin.mclists.cn/skin/skins/2d4846a1894fb6d8b03e00b10bff4c6854d3644edcb4f524c0db0f6c1940df3f.png",
            "https://skin.mclists.cn/skin/skins/5e5dcc9262f995e0e8b3d0939b878ee5b56f567b951fb058bb4b98e636123184.png",
            "https://skin.mclists.cn/skin/skins/9e769ef5baab873f1a764c595b6700de70a0a363ec413868589dce0f06442a69.png",
            "https://skin.mclists.cn/skin/skins/7a8fb3cd830e798fd60737ce14ff59fc1c2450fcb693a240ec5532522c985079.png",
            "https://skin.mclists.cn/skin/skins/aff77cba1cec58dd2ca827fcfe5bfb7347b6fe300dbbcfb9c69fb14aac772667.png",
            "https://skin.mclists.cn/skin/skins/1e8019b795372aa6a3be8dfd21318a3e516ba616b242741463c3fe5662cc1135.png",
            "https://skin.mclists.cn/skin/skins/2c10b2a675d17d6f239f45934202ac333926379ab489a8c4d54e62ea6121f8e9.png",
            "https://skin.mclists.cn/skin/skins/48afb79fb36ab765dcb001fc7e08da325934eec03693e06b45f6242905ebb4b.png",
            "https://skin.mclists.cn/skin/skins/1b23ebb6da7b7b882170ca64b8e6a9c4a08b38628dc7ef1eef7c1443837e72fa.png",
            "https://skin.mclists.cn/skin/skins/492e2cfad7e323a836c445d37e60b4b7a3571e189f87524356300d475dabc9ed.png",
            "https://skin.mclists.cn/skin/skins/5144b56bb3dd58bd26a7fcec2ce0f0a01e349ff05ebe086c2ac9773baf781c9.png",
            "https://skin.mclists.cn/skin/skins/eca07c6a9b26612ad6fc4f58877b403f3975edb64179e81d55f2a23bc1da2173.png",
            "https://skin.mclists.cn/skin/skins/7e3a4f1bf35a42f28cb3be94a9f0934cff6c37c2921589ed680f0dabc2c65343.png",
            "https://skin.mclists.cn/skin/skins/6d0c6ad8cfbabf30ae11fcd26041a2d4d91cd7caeccc49ee4e9e81632b0cd3c2.png",
            "https://skin.mclists.cn/skin/skins/b5bb8c67b113d64c35a0d11fea5220a007f709c854007e2221bb03bde4f0c21c.png",
            "https://skin.mclists.cn/skin/skins/839e35ad17a2c4673283d9538135d22b8b131fb7167de8138c1c2ebf1e10ff3.png",
            "https://skin.mclists.cn/skin/skins/c08ffc334dfdf15635415483f02cfd1bfcfa35173a17d351b33ad4df8299c804.png",
            "https://skin.mclists.cn/skin/skins/efea2d244a0ffe6cb9dc4b550d3ef2e86dd923bf24e69116f7920787f230aebd.png",
            "https://skin.mclists.cn/skin/skins/d67110e31f13d34c7b22250de4c00b42e96e0c5fe54baddea820de7ff190abc7.png",
            "https://skin.mclists.cn/skin/skins/d4d716da12a986fb3a5d0a716fc9b730ca259a6acf4e77af03eefd1d37ac8bdf.png",
            "https://skin.mclists.cn/skin/skins/679d34e8bdeab81c686341b262b402cf5f36ffb87095b16ceaa38f0b07866ae4.png",
            "https://skin.mclists.cn/skin/skins/408238a839b06d25fc8ea7560c1f2273e480deb08a826b676bfc707115bc3ca1.png",
            "https://skin.mclists.cn/skin/skins/5f2381ea4eab95086f7e3f22a4c20567343e0bf68633726c24ad5bd476ef6886.png",
            "https://skin.mclists.cn/skin/skins/75bf9362e5d1f7fe15906ac0f58e3e0226e9cc4cf01f4776dc660ad8888bedfc.png",
            "https://skin.mclists.cn/skin/skins/73aac547fd8d8991b4aca3eabe550c80486251c393c791120801420dde14374.png",
            "https://skin.mclists.cn/skin/skins/2d2e2c98ddcd9f978b668e8359e3e1041f1214e9fb815b6fdb014da55b66358f.png",
            "https://skin.mclists.cn/skin/skins/6eedf164b500a63149fecb21b590524eda532c20d60976656c6f91e11f2f8223.png",
            "https://skin.mclists.cn/skin/skins/ff7c428c7049fa55f94269cf04bd9dd9954af4c376661e76e332de7ac56a870e.png",
            "https://skin.mclists.cn/skin/skins/1e9ec072b6781731596bbab99d9f0fcc02f83e3ca54af3f274e3628a681da73b.png",
            "https://skin.mclists.cn/skin/skins/60ceb1c3263f4b3f6153fb86dbf81979e4136476e88c2d0c83d604ddd2d33ad4.png",
            "https://skin.mclists.cn/skin/skins/6383326873acd32de8987295ea834ef71f00aebf660786a7b2955de11cc0b5c7.png",
            "https://skin.mclists.cn/skin/skins/ddbd3c0f3fd5b9ea1d411bd483f2e3530a24fcdf224ca629533489c684162670.png",
            "https://skin.mclists.cn/skin/skins/f0943c3c5218dc3768d44e913b7c5964f80840bf80547453da7e2a57a36d1c6.png",
            "https://skin.mclists.cn/skin/skins/e56d3fe01aea9ab0ba577c5c2be274935b27a8dc20e670ed5d4411bb99b3b62e.png",
            "https://skin.mclists.cn/skin/skins/658c3eae8bcb92db8fff691d13af21f55a252e03f5d9f03ac982fa29a0a08860.png",
            "https://skin.mclists.cn/skin/skins/aa9b16abb6f821cf997ec87c2bce9d7cf843b2d962720ed6976709d407ba5620.png",
            "https://skin.mclists.cn/skin/skins/9b2c9aeb185bafc19b108eae91ade6f6d7e37152a3bb9d18c8ecdce48e540d30.png",
            "https://skin.mclists.cn/skin/skins/18520871d32364a4428f68d4e9bc1714126a3a41b9e1475be073be423157e44d.png",
            "https://skin.mclists.cn/skin/skins/b3fc21d5858f0746293ea83c9c2c41a86af16a1bb82bbc3cccdf63087a5f3dff.png",
            "https://skin.mclists.cn/skin/skins/b19955652e2a8f0093296d971aac5d79e498f50d00f8c263601346c8505e59c9.png",
            "https://skin.mclists.cn/skin/skins/3b0f0ff252690145aba88d88edd867a4a89fae2394b88cb27310aab3ac8a45f6.png",
            "https://skin.mclists.cn/skin/skins/7148c873d0cdbdbf3d148001e5efd253dd855c8f11df76d5ba408912144b2bd7.png",
            "https://skin.mclists.cn/skin/skins/4f36759d68a377490f03e683967a62f26edfd1bad532efdfa76c80abda31b4e6.png",
            "https://skin.mclists.cn/skin/skins/c6b02f85feef152b2fbfe8b3de315a92b00f225216e41712ee3395adfe65b23f.png",
            "https://skin.mclists.cn/skin/skins/2d0d6a5cd2edfc86f1be3ae81823ac0dc7f8e8387ec1e5d293a228c804635ec8.png",
            "https://skin.mclists.cn/skin/skins/eb64f84b8c75573c262a0e9d646d4e0991789cfa0658b45b8829eeb79be2d264.png",
            "https://skin.mclists.cn/skin/skins/413681fda104c582ce1e5271f7bda8c2aff3edde386aa3dfe68807326cb0d12d.png",
            "https://skin.mclists.cn/skin/skins/7593f994cf036bc5833d38c9506675f0342234aea450270517bca523365a3c7b.png",
            "https://skin.mclists.cn/skin/skins/8e60a4deb49cbfc6471a64fc441973318ad4a4698c12195977eada5811f5543f.png",
            "https://skin.mclists.cn/skin/skins/ae94e251266b0fede4a49f5e89e1570ab5e0f8227b38249017e4dd379291c4b3.png",
            "https://skin.mclists.cn/skin/skins/61011a89feba54df2edbeb9f82c032699270304758230d87355caaa4a783fc73.png",
            "https://skin.mclists.cn/skin/skins/960d0693021a8968b883a4ad1e71c1b0b0cd1bcba383b62775ccb3d517dc7369.png",
            "https://skin.mclists.cn/skin/skins/10d3cf2e90df2d3297380c1416c704b29b62a1af37b1ad7eacd82ea40ed04959.png",
            "https://skin.mclists.cn/skin/skins/893e6b828f15b081584e047f482bf171798fe71b9372b3865954be1609735a5c.png",
            "https://skin.mclists.cn/skin/skins/7a662e6d7e72734fc6d1ce8b8749d2ff7beb5016867bb66890222ae7986a525e.png",
            "https://skin.mclists.cn/skin/skins/d5ae082afc0e61932fb139d4da7f185c69110561dd667f122c382e0cbe92c45.png",
            "https://skin.mclists.cn/skin/skins/3368a4b521cb8da3726bb688c3a76344f84697762e938427edabc2e6b8a52c3d.png",
            "https://skin.mclists.cn/skin/skins/741f19415ec1e224fec70dfbc563ae076d5858cfb8402f495b4c5c719a7bae30.png",
            "https://skin.mclists.cn/skin/skins/77443f1f15f97e1bb76882b34072c2c70e23b9d140c6ff281453f90a4a9b2f6e.png",
            "https://skin.mclists.cn/skin/skins/6568e0a75f3e51a11754989afb3ba495edec6bba691889d6340014d7e567673b.png",
            "https://skin.mclists.cn/skin/skins/570c0880f5910d8c884c527c344e357f4b59e980d71add34dc8576b38008ec1b.png",
            "https://skin.mclists.cn/skin/skins/4494c10b538ad1598f4adc4cf99f70ef86d24929e912d240605e58628cc8229c.png",
            "https://skin.mclists.cn/skin/skins/390dc00d81f0d8e61dee86dcf39740b735bdbbea79183c89d7a44c85c7b63e8a.png",
            "https://skin.mclists.cn/skin/skins/8b3c884add317e82f7c7929a706831a9bc5b22bdc7495c79c2dd4c188e631309.png",
            "https://skin.mclists.cn/skin/skins/8c084015eba42f1a23aaa03a1f5e5836406f3393e405d490e7ed6d16b3c6279a.png",
            "https://skin.mclists.cn/skin/skins/862b3d8454d761c1ac9b45b2e05f93d4332666023e1d8a1a3beb52b34e4ba77b.png",
            "https://skin.mclists.cn/skin/skins/9c290c9dbc452509f87435522874192aa49d879523a073fda30391f5c3850c9c.png",
            "https://skin.mclists.cn/skin/skins/a0a23939d6cfb9fbbcda3f079961e9955c6965fcfcfd6241152077204b3d5b16.png",
            "https://skin.mclists.cn/skin/skins/d97cab230b44b7945bf4df7d41165655b96bae8b540e6d8457996f12bb4bb5ad.png",
            "https://skin.mclists.cn/skin/skins/73a8f2de0dcf8a03ff8077bbbcfe0111863c669aa9e480e867d88d6ede959273.png",
            "https://skin.mclists.cn/skin/skins/3217d18f083471e792cddec9ac5cdcb86f3126b5ec2972c26890c1dac51eeba.png",
            "https://skin.mclists.cn/skin/skins/643c2fe8cc41711ef35916d94f5609c065f8b87d088d38e238e5a141e20ba962.png",
            "https://skin.mclists.cn/skin/skins/19351b0ccd05d2c17540bef81ecd2cc00d282962e96f2e6e82b3287b49fa7b45.png",
            "https://skin.mclists.cn/skin/skins/8598c4fa910efb34322a32bd41cbd30e3ee5fea7201214b9c2eb94bb50b01eba.png"
    };

    @Override
    public List<PlayerInfo> getPlayerList() {
        List<PlayerInfo> mockList = new ArrayList<>();
        Random random = new Random();
        int count = 100 + random.nextInt(21);

        for (int i = 0; i < count; i++) {
            PlayerInfo info = new PlayerInfo();
            info.setPlayerName("Player_" + (1000 + i));
            info.setPlayerUuid(UUID.randomUUID().toString());
            String ip = random.nextInt(256) + "." +
                    random.nextInt(256) + "." +
                    random.nextInt(256) + "." +
                    random.nextInt(256);
            info.setIp(ip);
            int skinIndex = random.nextInt(PREDEFINED_SKINS.length);
            info.setSkinUrl(PREDEFINED_SKINS[skinIndex]);
            info.setBedrock(random.nextBoolean());
            mockList.add(info);
        }

        return mockList;
    }
}
