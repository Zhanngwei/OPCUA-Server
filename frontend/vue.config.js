const { defineConfig } = require('@vue/cli-service')

module.exports = defineConfig({
  transpileDependencies: true,
  devServer: {
    port: 3001,
    proxy: {
      '/api': {
        target: 'http://localhost:8166',
        changeOrigin: true,
        secure: false,
        logLevel: 'debug'
      }
    }
  },
  publicPath: process.env.NODE_ENV === 'production' ? './' : '/'
})
