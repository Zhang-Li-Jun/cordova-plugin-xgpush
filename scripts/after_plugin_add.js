var helper = require('./helper');

module.exports = function (context) {
    var fs = require('fs');
    var path = require('path');
    var searchFile = function (name, dir) {
        fs.readdir(dir, function (err, files) {
            if (err) {
                return console.error(err);
            } else {
                var tDir = dir;
                for (var i = 0; i < files.length; i++) {
                    if (files[i] === name) {
                        modifyIt(tDir + "/" + name);
                    } else if (files[i].indexOf('.java') === -1) {
                        searchFile(name, tDir + "/" + files[i]);
                    }
                }

            }
        })
    }

    var modifyIt = function (file) {
        var data = fs.readFileSync(file, 'utf-8');
        console.log("-----modifyIt-------" + JSON.stringify(context.opts.cli_variables));
        if(!context.opts.cli_variables) {
            return;
        }
        if(!context.opts.cli_variables.ACCESS_ID) {
            return;
        }
        console.log("-----modifyIt-------ACCESS_ID" + JSON.stringify(context.opts.cli_variables.ACCESS_ID));
        data = data.replace('ANDROID_ACCESS_ID', context.opts.cli_variables.ACCESS_ID);
        data = data.replace('ANDROID_ACCESS_KEY', context.opts.cli_variables.ACCESS_KEY);
        data = data.replace('xiaomi_package_name', context.opts.cli_variables.PACKAGE_NAME);
        data = data.replace('hw_appid', context.opts.cli_variables.HW_APPID);
        fs.writeFile(file, data, 'utf-8', function (err) {
            if (err) {
                return console.log("Insert android access_id and access_key error" + err);
            } else {
                return console.info('Insert android access_id and access_key success');
            }
        });
    }
    var getFileName = function () {
        var configStr = fs.readFileSync("config.xml", "utf-8");
        var tmpStr = configStr.match(/id=\s*([^;]*)/)[0];
        var strs = tmpStr.split(' ')[0].split(".");
        var str = strs[strs.length - 1];
        return str.substr(0, str.length - 1) + "-build-extras.gradle";
    }
    searchFile(getFileName(), 'platforms/android/cordova-plugin-xgpush');

    helper.restoreRootBuildGradle();
    helper.modifyRootBuildGradle();
}