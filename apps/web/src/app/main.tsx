import React from 'react'
import ReactDOM from 'react-dom/client'
import { Providers } from './providers'
import { AppRouter } from './router'
import '../index.css'

ReactDOM.createRoot(document.getElementById('root')!).render(
  <React.StrictMode>
    <Providers>
      <AppRouter />
    </Providers>
  </React.StrictMode>,
)
