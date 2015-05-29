prebuilt_jar(
  name = 'android-support-v4',
  binary_jar = './libs/android-support-v4.jar',
  visibility = [ 'PUBLIC' ],
)

keystore(
  name = 'debug_keystore',
  store = './keystore/debug.keystore',
  properties = './keystore/debug.keystore.properties',
)

android_binary(
  name = 'debug',
  package_type = 'DEBUG',
  manifest = './AndroidManifest.xml',
  keystore = ':debug_keystore',
  deps = [
    ':res',
    ':src',
  ],
)

android_resource(
  name = 'res',
  res = './res',
  assets = './assets',
  package = 'com.keger.utils',
  deps = [],
  visibility = [ 'PUBLIC' ],
)

android_library(
  name = 'src',
  srcs = glob(['./src/**/*.java']),
  deps = [
    ':build_config',
    ':res',
    ':android-support-v4',
  ],
)

android_build_config(
  name = 'build_config',
  package = 'com.keger.utils',
)