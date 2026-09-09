using System.IO;
using UnityEditor;
using UnityEditor.Build.Reporting;
using UnityEditor.SceneManagement;
using UnityEngine;

public static class FutRealBuild
{
    public static void BuildAndroid()
    {
        Directory.CreateDirectory("Assets/Generated");
        Directory.CreateDirectory("Builds/Android");

        var scene = EditorSceneManager.NewScene(NewSceneSetup.EmptyScene, NewSceneMode.Single);
        var root = new GameObject("futREAL");
        root.AddComponent<FutRealBootstrap>();
        string scenePath = "Assets/Generated/Main.unity";
        EditorSceneManager.SaveScene(scene, scenePath);

        EditorBuildSettings.scenes = new[] { new EditorBuildSettingsScene(scenePath, true) };

        PlayerSettings.productName = "futREAL";
        PlayerSettings.companyName = "NOX";
        PlayerSettings.SetApplicationIdentifier(BuildTargetGroup.Android, "com.nox.futreal");
        PlayerSettings.bundleVersion = "6.0";
        PlayerSettings.Android.bundleVersionCode = 60;
        PlayerSettings.Android.minSdkVersion = AndroidSdkVersions.AndroidApiLevel21;
        PlayerSettings.defaultInterfaceOrientation = UIOrientation.LandscapeLeft;
        PlayerSettings.colorSpace = ColorSpace.Linear;
        PlayerSettings.graphicsJobs = false;
        PlayerSettings.SetScriptingBackend(BuildTargetGroup.Android, ScriptingImplementation.Mono2x);
        PlayerSettings.Android.targetArchitectures = AndroidArchitecture.ARMv7 | AndroidArchitecture.ARM64;
        EditorUserBuildSettings.buildAppBundle = false;

        CreateIcon();

        var options = new BuildPlayerOptions
        {
            scenes = new[] { scenePath },
            locationPathName = "Builds/Android/futREAL-Unity.apk",
            target = BuildTarget.Android,
            options = BuildOptions.None
        };

        BuildReport report = BuildPipeline.BuildPlayer(options);
        if (report.summary.result != BuildResult.Succeeded)
            throw new System.Exception("futREAL Unity Android build failed: " + report.summary.result);

        Debug.Log("futREAL Unity APK created: " + options.locationPathName);
    }

    static void CreateIcon()
    {
        const int size = 256;
        var tex = new Texture2D(size, size, TextureFormat.RGBA32, false);
        Color bg = new Color(0.025f, 0.04f, 0.055f, 1f);
        Color neon = new Color(0.70f, 1f, 0.16f, 1f);
        Color white = new Color(0.96f, 0.98f, 1f, 1f);

        Vector2 center = new Vector2(size / 2f, size / 2f);
        for (int y = 0; y < size; y++)
        for (int x = 0; x < size; x++)
        {
            float d = Vector2.Distance(new Vector2(x, y), center);
            Color c = bg;
            if (d < 88f) c = neon;
            if (d < 62f) c = bg;
            if ((x > 72 && x < 184 && y > 121 && y < 135) ||
                (x > 72 && x < 84 && y > 92 && y < 164) ||
                (x > 172 && x < 184 && y > 92 && y < 164)) c = white;
            tex.SetPixel(x, y, c);
        }
        tex.Apply();

        string path = "Assets/Generated/futreal_icon.png";
        File.WriteAllBytes(path, tex.EncodeToPNG());
        Object.DestroyImmediate(tex);
        AssetDatabase.ImportAsset(path, ImportAssetOptions.ForceUpdate);
        var icon = AssetDatabase.LoadAssetAtPath<Texture2D>(path);
        PlayerSettings.SetIconsForTargetGroup(BuildTargetGroup.Android, new[] { icon });
        AssetDatabase.SaveAssets();
    }
}
