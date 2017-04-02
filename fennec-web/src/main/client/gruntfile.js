var rework_import = require('rework-import');
module.exports = function (grunt) {
    grunt.initConfig({
        clean: {
            options: {
                force: true
            },
            stuff: ['../resources/webroot/dist/js']
        },
        browserify: {
            '../resources/webroot/dist//js/bundle.js': ['js/index.js']
        },
        // rework: {
        //     '../resources/webroot/dist//css/bundle.css': 'css/index.css',
        //     options: {
        //         toString: {compress: true},
        //         use: [rework_import],
        //         vendors: ['-moz-', '-webkit-']
        //     },
        //     prod: {
        //         options: {
        //             toString: {
        //                 compress: true
        //             }
        //         },
        //         files: {
        //             '../resources/webroot/dist//css/bundle.css': 'css/index.css',
        //         }
        //     }
        // },
        copy: {
            images: {
                src: 'img/**',
                dest: 'dist/img',
                expand: true
            }
        },
    });

    grunt.loadNpmTasks('grunt-contrib-clean');
    grunt.loadNpmTasks('grunt-contrib-copy');
    grunt.loadNpmTasks('grunt-contrib-cssmin');
    grunt.loadNpmTasks('grunt-rework');
    grunt.loadNpmTasks('grunt-browserify');
    grunt.loadNpmTasks('grunt-bootstrap');

    grunt.registerTask('default', ['clean', 'browserify',
        // 'rework',
        'copy']);
};